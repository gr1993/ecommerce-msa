package com.example.productservice.consumer;

import com.example.productservice.consumer.event.InventoryDecreaseEvent;
import com.example.productservice.product.service.InventoryService;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Inventory 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 구독 이벤트:
 * - inventory.decrease: 교환 승인 시 신규 SKU 재고 차감
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryService inventoryService;

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "inventory.decrease",
                    description = "재고 차감 이벤트 구독 - 교환 승인 시 신규 SKU 재고 차감",
                    message = @AsyncMessage(
                            messageId = "inventoryDecreaseEvent",
                            name = "InventoryDecreaseEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-product-retry",
            dltTopicSuffix = "-product-dlt"
    )
    @KafkaListener(topics = "inventory.decrease", groupId = "${spring.kafka.consumer.group-id:product-service}")
    public void consumeInventoryDecreaseEvent(
            @Payload InventoryDecreaseEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received inventory.decrease event: orderId={}, exchangeId={}, reason={}, itemCount={}, topic={}, offset={}",
                event.getOrderId(), event.getExchangeId(), event.getReason(),
                event.getItems() != null ? event.getItems().size() : 0, topic, offset);

        try {
            inventoryService.decreaseStockForExchangeApproved(event);
            log.info("Successfully processed inventory.decrease event: orderId={}, exchangeId={}",
                    event.getOrderId(), event.getExchangeId());
        } catch (Exception e) {
            log.error("Failed to process inventory.decrease event: orderId={}, exchangeId={}",
                    event.getOrderId(), event.getExchangeId(), e);
            throw e;
        }
    }

    /**
     * DLQ(Dead Letter Queue) 핸들러
     * 모든 재시도가 실패한 후 호출됩니다.
     *
     * 실패 가능 원인:
     * - SKU를 찾을 수 없음
     * - 재고 부족 (교환 승인 시점과 재고 차감 시점 사이 재고 소진)
     * - DB 장애
     *
     * DLQ 메시지는 Kafka UI 또는 모니터링 도구를 통해 확인하고
     * 수동으로 재처리할 수 있습니다.
     */
    @DltHandler
    public void handleDlt(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(value = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
    ) {
        log.error("""
                ========================================
                DLQ 메시지 수신 (재시도 실패)
                ========================================
                DLT Topic: {}
                Original Topic: {}
                Offset: {}
                Payload: {}
                Exception: {}
                ========================================
                """, topic, originalTopic, offset, payload, exceptionMessage);

        if (payload instanceof InventoryDecreaseEvent event) {
            log.error("DLQ 처리 필요 - inventory.decrease 실패: orderId={}, exchangeId={}, reason={}, itemCount={}",
                    event.getOrderId(), event.getExchangeId(), event.getReason(),
                    event.getItems() != null ? event.getItems().size() : 0);
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.productservice.consumer;

import com.example.productservice.consumer.event.PaymentCancelledEvent;
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
 * Payment 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 구독 이벤트:
 * - payment.cancelled: 결제 실패/시간 초과 시 재고 복구 (보상 트랜잭션)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final InventoryService inventoryService;

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "payment.cancelled",
                    description = "결제 취소 이벤트 구독 - 재고 복구 보상 트랜잭션",
                    message = @AsyncMessage(
                            messageId = "paymentCancelledEvent",
                            name = "PaymentCancelledEvent"
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
    @KafkaListener(topics = "payment.cancelled", groupId = "${spring.kafka.consumer.group-id:product-service}")
    public void consumePaymentCancelledEvent(
            @Payload PaymentCancelledEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received payment.cancelled event: orderId={}, orderNumber={}, paymentId={}, reason={}, topic={}, offset={}",
                event.getOrderId(), event.getOrderNumber(), event.getPaymentId(),
                event.getCancellationReason(), topic, offset);

        try {
            inventoryService.restoreStockForPaymentCancelled(event);
            log.info("Successfully processed payment.cancelled event (compensation): orderId={}, orderNumber={}, paymentId={}, reason={}",
                    event.getOrderId(), event.getOrderNumber(), event.getPaymentId(), event.getCancellationReason());
        } catch (Exception e) {
            log.error("Failed to process payment.cancelled event: orderId={}, orderNumber={}, paymentId={}, reason={}",
                    event.getOrderId(), event.getOrderNumber(), event.getPaymentId(), event.getCancellationReason(), e);
            throw e;
        }
    }

    /**
     * DLQ(Dead Letter Queue) 핸들러
     * 모든 재시도가 실패한 후 호출됩니다.
     *
     * 실패 가능 원인:
     * - SKU를 찾을 수 없음
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

        if (payload instanceof PaymentCancelledEvent event) {
            log.error("DLQ 처리 필요 - payment.cancelled 실패: orderId={}, orderNumber={}, paymentId={}, reason={}, itemCount={}",
                    event.getOrderId(), event.getOrderNumber(), event.getPaymentId(),
                    event.getCancellationReason(), event.getItems().size());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.shippingservice.consumer;

import com.example.shippingservice.common.entity.ProcessedEvent;
import com.example.shippingservice.common.repository.ProcessedEventRepository;
import com.example.shippingservice.consumer.event.OrderCreatedEvent;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * order.created 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - processed_events 테이블의 event_id(order.created:{orderId}) 기반 중복 체크로
 *   동일 이벤트가 중복 수신되어도 한 번만 처리됨을 보장함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private static final String EVENT_TYPE = "order.created";

    private final OrderShippingRepository orderShippingRepository;
    private final ProcessedEventRepository processedEventRepository;

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
            retryTopicSuffix = "-shipping-retry",
            dltTopicSuffix = "-shipping-dlt"
    )
    @KafkaListener(topics = "order.created", groupId = "${spring.kafka.consumer.group-id:shipping-service}")
    @Transactional
    public void consumeOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received order.created event: orderId={}, orderNumber={}, topic={}, offset={}",
                event.getOrderId(), event.getOrderNumber(), topic, offset);

        try {
            String eventId = EVENT_TYPE + ":" + event.getOrderId();

            // 멱등성 보장: 이미 처리된 이벤트 중복 처리 방지
            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Already processed event, skipping: eventId={}", eventId);
                return;
            }

            OrderCreatedEvent.DeliverySnapshot delivery = event.getDelivery();
            String fullAddress = buildFullAddress(delivery);

            OrderShipping orderShipping = OrderShipping.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .userId(event.getUserId())
                    .receiverName(delivery.getReceiverName())
                    .receiverPhone(delivery.getReceiverPhone())
                    .address(fullAddress)
                    .postalCode(delivery.getZipcode())
                    .shippingStatus(ShippingStatus.READY)
                    .deliveryServiceStatus(DeliveryServiceStatus.NOT_SENT)
                    .build();
            orderShippingRepository.save(orderShipping);

            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .eventType(EVENT_TYPE)
                    .processedAt(LocalDateTime.now())
                    .build());

            log.info("Successfully processed order.created event: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to process order.created event: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber(), e);
            throw e;
        }
    }

    private String buildFullAddress(OrderCreatedEvent.DeliverySnapshot delivery) {
        if (delivery.getAddressDetail() == null || delivery.getAddressDetail().isBlank()) {
            return delivery.getAddress();
        }
        return delivery.getAddress() + " " + delivery.getAddressDetail();
    }

    /**
     * DLQ(Dead Letter Queue) 핸들러
     * 모든 재시도가 실패한 후 호출됩니다.
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

        if (payload instanceof OrderCreatedEvent event) {
            log.error("DLQ 처리 필요 - order.created 실패: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.shippingservice.consumer;

import com.example.shippingservice.common.entity.ProcessedEvent;
import com.example.shippingservice.common.repository.ProcessedEventRepository;
import com.example.shippingservice.consumer.event.OrderCancelledEvent;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import com.example.shippingservice.shipping.service.MockDeliveryService;
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
import java.util.Optional;

/**
 * order.cancelled 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 *
 * 멱등성:
 * - processed_events 테이블의 event_id(order.cancelled:{orderId}) 기반 중복 체크
 *
 * 처리 흐름:
 * - NOT_SENT: Mock API 호출 없이 배송 상태만 CANCELLED로 변경
 * - SENT: Mock API bulk-cancel 호출 후 배송 상태 CANCELLED로 변경
 * - IN_TRANSIT 이상: Feign 체크로 사전 차단되어야 하나, 도달 시 경고 로그만 기록
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelledEventConsumer {

    private static final String EVENT_TYPE = "order.cancelled";

    private final OrderShippingRepository orderShippingRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final MockDeliveryService mockDeliveryService;

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
    @KafkaListener(topics = "order.cancelled", groupId = "${spring.kafka.consumer.group-id:shipping-service}")
    @Transactional
    public void consumeOrderCancelledEvent(
            @Payload OrderCancelledEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received order.cancelled event: orderId={}, orderNumber={}, topic={}, offset={}",
                event.getOrderId(), event.getOrderNumber(), topic, offset);

        try {
            String eventId = EVENT_TYPE + ":" + event.getOrderId();

            if (processedEventRepository.existsByEventId(eventId)) {
                log.info("Already processed event, skipping: eventId={}", eventId);
                return;
            }

            Optional<OrderShipping> optShipping = orderShippingRepository.findByOrderId(event.getOrderId());

            if (optShipping.isEmpty()) {
                log.warn("배송 레코드 없음 - 건너뜀: orderId={}", event.getOrderId());
                saveProcessedEvent(eventId);
                return;
            }

            OrderShipping shipping = optShipping.get();

            if (shipping.getShippingStatus() == ShippingStatus.CANCELLED
                    || shipping.getShippingStatus() == ShippingStatus.RETURNED) {
                log.info("이미 취소/반품 완료된 배송 - 건너뜀: orderId={}, status={}",
                        event.getOrderId(), shipping.getShippingStatus());
                saveProcessedEvent(eventId);
                return;
            }

            if (shipping.getDeliveryServiceStatus() == DeliveryServiceStatus.IN_TRANSIT
                    || shipping.getDeliveryServiceStatus() == DeliveryServiceStatus.DELIVERED) {
                // Feign 체크로 사전 차단되어야 하는 케이스
                log.error("취소 불가 상태에서 order.cancelled 이벤트 수신 - orderId={}, deliveryServiceStatus={}",
                        event.getOrderId(), shipping.getDeliveryServiceStatus());
                saveProcessedEvent(eventId);
                return;
            }

            // SENT 상태: Mock API에 취소 요청
            if (shipping.getDeliveryServiceStatus() == DeliveryServiceStatus.SENT) {
                log.info("Mock API 배송 취소 요청 - orderId={}, trackingNumber={}",
                        event.getOrderId(), shipping.getTrackingNumber());
                boolean cancelled = mockDeliveryService.cancelSingleTrackingNumber(shipping.getTrackingNumber());
                if (!cancelled) {
                    log.warn("Mock API 배송 취소 실패 - orderId={}, trackingNumber={}. 배송 상태는 CANCELLED로 처리합니다.",
                            event.getOrderId(), shipping.getTrackingNumber());
                }
            }

            // 배송 상태 CANCELLED로 변경
            shipping.updateShippingStatus(ShippingStatus.CANCELLED, "ORDER_CANCELLED");
            orderShippingRepository.save(shipping);

            saveProcessedEvent(eventId);

            log.info("배송 취소 처리 완료: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to process order.cancelled event: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber(), e);
            throw e;
        }
    }

    private void saveProcessedEvent(String eventId) {
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .processedAt(LocalDateTime.now())
                .build());
    }

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

        if (payload instanceof OrderCancelledEvent event) {
            log.error("DLQ 처리 필요 - order.cancelled 실패: orderId={}, orderNumber={}",
                    event.getOrderId(), event.getOrderNumber());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.ShippingDeliveredEvent;
import com.example.orderservice.consumer.event.ShippingStartedEvent;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
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

/**
 * 배송 이벤트 컨슈머
 *
 * Shipping Service에서 발행하는 배송 출발/완료 이벤트를 소비한다.
 * - shipping.started: 주문 상태를 SHIPPING으로 변경
 * - shipping.delivered: 주문 상태를 DELIVERED로 변경
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 *
 * 멱등성:
 * - 배송 출발: 이미 SHIPPING 이상 상태(SHIPPING, DELIVERED, CANCELED 등)면 skip
 * - 배송 완료: 이미 DELIVERED 또는 CANCELED 상태면 skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventConsumer {

    private final OrderRepository orderRepository;

    /**
     * 배송 출발 이벤트 처리
     *
     * 주문 상태를 SHIPPING으로 변경한다.
     * 멱등성: PAID 상태에서만 SHIPPING으로 변경 가능
     */
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
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order-dlt"
    )
    @KafkaListener(topics = "shipping.started", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeShippingStartedEvent(
            @Payload ShippingStartedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received shipping.started event: shippingId={}, orderId={}, trackingNumber={}, topic={}, offset={}",
                event.getShippingId(), event.getOrderId(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for shipping started: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: PAID 상태에서만 SHIPPING으로 변경
            if (order.getOrderStatus() != OrderStatus.PAID) {
                log.info("주문 상태가 PAID가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → SHIPPING 변경
            order.updateStatus(OrderStatus.SHIPPING);
            orderRepository.save(order);

            log.info("배송 출발 처리 성공: orderId={}, shippingId={}, trackingNumber={}",
                    event.getOrderId(), event.getShippingId(), event.getTrackingNumber());
        } catch (Exception e) {
            log.error("Failed to process shipping.started event: shippingId={}, orderId={}",
                    event.getShippingId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * 배송 완료 이벤트 처리
     *
     * 주문 상태를 DELIVERED로 변경한다.
     * 멱등성: SHIPPING 상태에서만 DELIVERED로 변경 가능
     */
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
            retryTopicSuffix = "-order-retry",
            dltTopicSuffix = "-order-dlt"
    )
    @KafkaListener(topics = "shipping.delivered", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeShippingDeliveredEvent(
            @Payload ShippingDeliveredEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received shipping.delivered event: shippingId={}, orderId={}, trackingNumber={}, topic={}, offset={}",
                event.getShippingId(), event.getOrderId(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for shipping delivered: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: 이미 DELIVERED 또는 CANCELED 상태이면 skip
            if (order.getOrderStatus() == OrderStatus.DELIVERED
                    || order.getOrderStatus() == OrderStatus.CANCELED) {
                log.info("주문이 이미 {} 상태 - 건너뜀: orderId={}",
                        order.getOrderStatus(), event.getOrderId());
                return;
            }

            // 주문 상태 → DELIVERED 변경
            order.updateStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            log.info("배송 완료 처리 성공: orderId={}, shippingId={}, trackingNumber={}",
                    event.getOrderId(), event.getShippingId(), event.getTrackingNumber());
        } catch (Exception e) {
            log.error("Failed to process shipping.delivered event: shippingId={}, orderId={}",
                    event.getShippingId(), event.getOrderId(), e);
            throw e;
        }
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

        if (payload instanceof ShippingStartedEvent event) {
            log.error("DLQ 처리 필요 - shipping.started 실패: shippingId={}, orderId={}",
                    event.getShippingId(), event.getOrderId());
        } else if (payload instanceof ShippingDeliveredEvent event) {
            log.error("DLQ 처리 필요 - shipping.delivered 실패: shippingId={}, orderId={}",
                    event.getShippingId(), event.getOrderId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

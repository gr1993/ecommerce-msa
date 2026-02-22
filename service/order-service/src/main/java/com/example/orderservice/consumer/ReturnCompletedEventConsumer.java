package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.ReturnCompletedEvent;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.event.OrderCancelledEvent;
import com.example.orderservice.global.common.EventTypeConstants;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * return.completed 이벤트 컨슈머
 *
 * Shipping Service에서 관리자가 반품 완료 처리 시 발행되는 이벤트를 소비한다.
 * 주문 상태를 RETURNED로 변경하고, order.cancelled Outbox를 저장하여
 * 환불(Payment Service) + 재고 복구(Product Service)를 트리거한다.
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 *
 * 멱등성:
 * - 주문 상태가 이미 RETURNED 또는 CANCELED이면 skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnCompletedEventConsumer {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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
    @KafkaListener(topics = "return.completed", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeReturnCompletedEvent(
            @Payload ReturnCompletedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received return.completed event: returnId={}, orderId={}, topic={}, offset={}",
                event.getReturnId(), event.getOrderId(), topic, offset);

        try {
            Order order = orderRepository.findByIdWithOrderItems(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for return completion: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: 이미 RETURNED 또는 CANCELED 상태이면 skip
            if (order.getOrderStatus() == OrderStatus.RETURNED
                    || order.getOrderStatus() == OrderStatus.CANCELED) {
                log.info("주문이 이미 {}  상태 - 건너뜀: orderId={}",
                        order.getOrderStatus(), event.getOrderId());
                return;
            }

            // 주문 상태 → RETURNED 변경
            order.updateStatus(OrderStatus.RETURNED);
            orderRepository.save(order);

            // order.cancelled Outbox 저장 (환불 + 재고 복구 트리거)
            saveOrderCancelledOutbox(order, "RETURN_COMPLETED");

            log.info("반품 완료 처리 성공: orderId={}, returnId={}", event.getOrderId(), event.getReturnId());
        } catch (Exception e) {
            log.error("Failed to process return.completed event: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId(), e);
            throw e;
        }
    }

    private void saveOrderCancelledOutbox(Order order, String cancellationReason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .cancellationReason(cancellationReason)
                .userId(order.getUserId())
                .cancelledItems(order.getOrderItems().stream()
                        .map(item -> OrderCancelledEvent.CancelledOrderItem.builder()
                                .orderItemId(item.getId())
                                .productId(item.getProductId())
                                .skuId(item.getSkuId())
                                .productName(item.getProductName())
                                .productCode(item.getProductCode())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .cancelledAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(String.valueOf(order.getId()))
                    .eventType(EventTypeConstants.TOPIC_ORDER_CANCELLED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("OrderCancelledEvent Outbox 저장 완료 (반품): orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("OrderCancelledEvent 직렬화 실패: orderId={}", order.getId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
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

        if (payload instanceof ReturnCompletedEvent event) {
            log.error("DLQ 처리 필요 - return.completed 실패: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

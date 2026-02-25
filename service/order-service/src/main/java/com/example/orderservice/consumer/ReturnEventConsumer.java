package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.ReturnApprovedEvent;
import com.example.orderservice.consumer.event.ReturnCompletedEvent;
import com.example.orderservice.consumer.event.ReturnInTransitEvent;
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
 * 반품 관련 이벤트 통합 컨슈머
 *
 * Shipping Service에서 발행되는 반품 관련 이벤트들을 소비하여 주문 상태를 업데이트한다.
 *
 * 처리 이벤트:
 * 1. return.approved - 관리자가 반품 승인 처리 시 → RETURN_APPROVED
 * 2. return.in-transit - 반품 물품 수거 중 → RETURN_IN_TRANSIT
 * 3. return.completed - 관리자가 반품 완료 처리 시 → RETURNED + 환불/재고복구 트리거
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventConsumer {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * return.approved 이벤트 소비
     *
     * 멱등성: RETURN_REQUESTED 상태에서만 RETURN_APPROVED로 변경
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
    @KafkaListener(topics = "return.approved", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeReturnApprovedEvent(
            @Payload ReturnApprovedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received return.approved event: returnId={}, orderId={}, courier={}, trackingNumber={}, topic={}, offset={}",
                event.getReturnId(), event.getOrderId(), event.getCourier(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for return approval: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: RETURN_REQUESTED 상태에서만 RETURN_APPROVED로 변경
            if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
                log.info("주문 상태가 RETURN_REQUESTED가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → RETURN_APPROVED 변경
            order.updateStatus(OrderStatus.RETURN_APPROVED);
            orderRepository.save(order);

            log.info("반품 승인 처리 성공: orderId={}, returnId={}, courier={}, trackingNumber={}",
                    event.getOrderId(), event.getReturnId(), event.getCourier(), event.getTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 반품이 승인되었으며 택배 기사가 물품을 회수하러 갈 예정임을 알림
            // - 회수 운송장 번호 제공

        } catch (Exception e) {
            log.error("Failed to process return.approved event: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * return.in-transit 이벤트 소비
     *
     * 멱등성: RETURN_APPROVED 상태에서만 RETURN_IN_TRANSIT으로 변경
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
    @KafkaListener(topics = "return.in-transit", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeReturnInTransitEvent(
            @Payload ReturnInTransitEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received return.in-transit event: returnId={}, orderId={}, courier={}, trackingNumber={}, topic={}, offset={}",
                event.getReturnId(), event.getOrderId(), event.getCourier(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for return in-transit: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: RETURN_APPROVED 상태에서만 RETURN_IN_TRANSIT으로 변경
            if (order.getOrderStatus() != OrderStatus.RETURN_APPROVED) {
                log.info("주문 상태가 RETURN_APPROVED가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → RETURN_IN_TRANSIT 변경
            order.updateStatus(OrderStatus.RETURN_IN_TRANSIT);
            orderRepository.save(order);

            log.info("반품 수거 중 처리 성공: orderId={}, returnId={}, courier={}, trackingNumber={}",
                    event.getOrderId(), event.getReturnId(), event.getCourier(), event.getTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 반품 물품이 수거되어 배송 중임을 알림

        } catch (Exception e) {
            log.error("Failed to process return.in-transit event: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * return.completed 이벤트 소비
     *
     * 멱등성: 이미 RETURNED 또는 CANCELED 상태이면 skip
     * 주문 상태를 RETURNED로 변경하고, order.cancelled Outbox를 저장하여
     * 환불(Payment Service) + 재고 복구(Product Service)를 트리거한다.
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

    /**
     * order.cancelled Outbox 저장
     *
     * Payment Service에서 환불 처리, Product Service에서 재고 복구 처리를 위한 이벤트 발행
     */
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

    /**
     * DLT 핸들러
     *
     * 모든 반품 관련 이벤트의 재시도가 실패했을 때 호출된다.
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

        if (payload instanceof ReturnApprovedEvent event) {
            log.error("DLQ 처리 필요 - return.approved 실패: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId());
        } else if (payload instanceof ReturnInTransitEvent event) {
            log.error("DLQ 처리 필요 - return.in-transit 실패: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId());
        } else if (payload instanceof ReturnCompletedEvent event) {
            log.error("DLQ 처리 필요 - return.completed 실패: returnId={}, orderId={}",
                    event.getReturnId(), event.getOrderId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

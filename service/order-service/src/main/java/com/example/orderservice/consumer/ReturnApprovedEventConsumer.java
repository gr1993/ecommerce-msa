package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.ReturnApprovedEvent;
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
 * return.approved 이벤트 컨슈머
 *
 * Shipping Service에서 관리자가 반품 승인 처리 시 발행되는 이벤트를 소비한다.
 * 주문 상태를 RETURN_APPROVED로 변경한다.
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 *
 * 멱등성:
 * - RETURN_REQUESTED 상태에서만 RETURN_APPROVED로 변경
 * - 이미 RETURN_APPROVED 이상 상태이면 skip
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnApprovedEventConsumer {

    private final OrderRepository orderRepository;

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
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.orderservice.consumer;

import com.example.orderservice.consumer.event.ExchangeApprovedEvent;
import com.example.orderservice.consumer.event.ExchangeCollectingEvent;
import com.example.orderservice.consumer.event.ExchangeCompletedEvent;
import com.example.orderservice.consumer.event.ExchangeReturnCompletedEvent;
import com.example.orderservice.consumer.event.ExchangeShippingEvent;
import com.example.orderservice.client.dto.ExchangeItemDto;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.event.InventoryDecreaseEvent;
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
import java.util.List;

/**
 * 교환 관련 이벤트 통합 컨슈머
 *
 * Shipping Service에서 발행되는 교환 관련 이벤트들을 소비하여 주문 상태를 업데이트한다.
 *
 * 처리 이벤트:
 * 1. exchange.collecting         - 기존 물품 회수 중 → EXCHANGE_COLLECTING
 * 2. exchange.return-completed   - 기존 물품 회수 완료 → EXCHANGE_RETURN_COMPLETED
 * 3. exchange.completed          - 교환 최종 완료 → EXCHANGED
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 → 2초 → 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeEventConsumer {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * exchange.approved 이벤트 소비
     *
     * 멱등성: EXCHANGE_REQUESTED 상태에서만 EXCHANGE_APPROVED로 변경
     * 신규 옵션이 있는 경우(originalOptionId != newOptionId) inventory.decrease 이벤트 발행
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
    @KafkaListener(topics = "exchange.approved", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeExchangeApprovedEvent(
            @Payload ExchangeApprovedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received exchange.approved event: exchangeId={}, orderId={}, collectCourier={}, collectTrackingNumber={}, topic={}, offset={}",
                event.getExchangeId(), event.getOrderId(), event.getCollectCourier(), event.getCollectTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findByIdWithOrderItems(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for exchange approval: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: EXCHANGE_REQUESTED 상태에서만 EXCHANGE_APPROVED로 변경
            if (order.getOrderStatus() != OrderStatus.EXCHANGE_REQUESTED) {
                log.info("주문 상태가 EXCHANGE_REQUESTED가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → EXCHANGE_APPROVED 변경
            order.updateStatus(OrderStatus.EXCHANGE_APPROVED);
            orderRepository.save(order);

            // 신규 옵션(newOptionId != originalOptionId)에 대한 재고 차감 이벤트 발행
            List<ExchangeItemDto> exchangeItems = event.getExchangeItems();
            if (exchangeItems != null && !exchangeItems.isEmpty()) {
                List<InventoryDecreaseEvent.DecreaseItem> decreaseItems = exchangeItems.stream()
                        .filter(item -> !item.getNewOptionId().equals(item.getOriginalOptionId()))
                        .map(item -> InventoryDecreaseEvent.DecreaseItem.builder()
                                .skuId(item.getNewOptionId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList();

                if (!decreaseItems.isEmpty()) {
                    saveInventoryDecreaseOutbox(order, event.getExchangeId(), decreaseItems);
                    log.info("inventory.decrease Outbox 저장 완료: orderId={}, exchangeId={}, 차감 항목 수={}",
                            event.getOrderId(), event.getExchangeId(), decreaseItems.size());
                } else {
                    log.info("신규 옵션 없음 - inventory.decrease 이벤트 발행 생략: orderId={}, exchangeId={}",
                            event.getOrderId(), event.getExchangeId());
                }
            }

            log.info("교환 승인 처리 성공: orderId={}, exchangeId={}, collectCourier={}, collectTrackingNumber={}",
                    event.getOrderId(), event.getExchangeId(), event.getCollectCourier(), event.getCollectTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 교환이 승인되었으며 회수 택배 기사가 기존 물품을 수거하러 갈 예정임을 알림
            // - 회수 운송장 번호 제공

        } catch (Exception e) {
            log.error("Failed to process exchange.approved event: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * exchange.collecting 이벤트 소비
     *
     * 멱등성: EXCHANGE_APPROVED 상태에서만 EXCHANGE_COLLECTING으로 변경
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
    @KafkaListener(topics = "exchange.collecting", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeExchangeCollectingEvent(
            @Payload ExchangeCollectingEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received exchange.collecting event: exchangeId={}, orderId={}, courier={}, trackingNumber={}, topic={}, offset={}",
                event.getExchangeId(), event.getOrderId(), event.getCourier(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for exchange collecting: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: EXCHANGE_APPROVED 상태에서만 EXCHANGE_COLLECTING으로 변경
            if (order.getOrderStatus() != OrderStatus.EXCHANGE_APPROVED) {
                log.info("주문 상태가 EXCHANGE_APPROVED가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → EXCHANGE_COLLECTING 변경
            order.updateStatus(OrderStatus.EXCHANGE_COLLECTING);
            orderRepository.save(order);

            log.info("교환 물품 회수 중 처리 성공: orderId={}, exchangeId={}, courier={}, trackingNumber={}",
                    event.getOrderId(), event.getExchangeId(), event.getCourier(), event.getTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 교환 물품 회수가 진행 중임을 알림

        } catch (Exception e) {
            log.error("Failed to process exchange.collecting event: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * exchange.shipping 이벤트 소비
     *
     * 멱등성: EXCHANGE_RETURN_COMPLETED 상태에서만 EXCHANGE_SHIPPING으로 변경
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
    @KafkaListener(topics = "exchange.shipping", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeExchangeShippingEvent(
            @Payload ExchangeShippingEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received exchange.shipping event: exchangeId={}, orderId={}, courier={}, trackingNumber={}, topic={}, offset={}",
                event.getExchangeId(), event.getOrderId(), event.getCourier(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for exchange shipping: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: EXCHANGE_RETURN_COMPLETED 상태에서만 EXCHANGE_SHIPPING으로 변경
            if (order.getOrderStatus() != OrderStatus.EXCHANGE_RETURN_COMPLETED) {
                log.info("주문 상태가 EXCHANGE_RETURN_COMPLETED가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → EXCHANGE_SHIPPING 변경
            order.updateStatus(OrderStatus.EXCHANGE_SHIPPING);
            orderRepository.save(order);

            log.info("교환 새 물품 배송 중 처리 성공: orderId={}, exchangeId={}, courier={}, trackingNumber={}",
                    event.getOrderId(), event.getExchangeId(), event.getCourier(), event.getTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 교환할 새 물품이 배송을 시작했음을 알림
            // - 새 물품 배송 운송장 번호 제공

        } catch (Exception e) {
            log.error("Failed to process exchange.shipping event: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * exchange.return-completed 이벤트 소비
     *
     * 멱등성: EXCHANGE_COLLECTING 상태에서만 EXCHANGE_RETURN_COMPLETED로 변경
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
    @KafkaListener(topics = "exchange.return-completed", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeExchangeReturnCompletedEvent(
            @Payload ExchangeReturnCompletedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received exchange.return-completed event: exchangeId={}, orderId={}, courier={}, trackingNumber={}, topic={}, offset={}",
                event.getExchangeId(), event.getOrderId(), event.getCourier(), event.getTrackingNumber(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for exchange return completed: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: EXCHANGE_COLLECTING 상태에서만 EXCHANGE_RETURN_COMPLETED로 변경
            if (order.getOrderStatus() != OrderStatus.EXCHANGE_COLLECTING) {
                log.info("주문 상태가 EXCHANGE_COLLECTING가 아님 - 건너뜀: orderId={}, currentStatus={}",
                        event.getOrderId(), order.getOrderStatus());
                return;
            }

            // 주문 상태 → EXCHANGE_RETURN_COMPLETED 변경
            order.updateStatus(OrderStatus.EXCHANGE_RETURN_COMPLETED);
            orderRepository.save(order);

            log.info("교환 물품 회수 완료 처리 성공: orderId={}, exchangeId={}, courier={}, trackingNumber={}",
                    event.getOrderId(), event.getExchangeId(), event.getCourier(), event.getTrackingNumber());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 기존 물품이 창고에 도착하여 검수 중임을 알림

        } catch (Exception e) {
            log.error("Failed to process exchange.return-completed event: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * exchange.completed 이벤트 소비
     *
     * 멱등성: 이미 EXCHANGED 또는 CANCELED 상태이면 skip
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
    @KafkaListener(topics = "exchange.completed", groupId = "${spring.kafka.consumer.group-id:order-service}")
    @Transactional
    public void consumeExchangeCompletedEvent(
            @Payload ExchangeCompletedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received exchange.completed event: exchangeId={}, orderId={}, topic={}, offset={}",
                event.getExchangeId(), event.getOrderId(), topic, offset);

        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for exchange completion: orderId={}", event.getOrderId());
                return;
            }

            // 멱등성 보장: 이미 EXCHANGED 또는 CANCELED 상태이면 skip
            if (order.getOrderStatus() == OrderStatus.EXCHANGED
                    || order.getOrderStatus() == OrderStatus.CANCELED) {
                log.info("주문이 이미 {} 상태 - 건너뜀: orderId={}",
                        order.getOrderStatus(), event.getOrderId());
                return;
            }

            // 주문 상태 → EXCHANGED 변경
            order.updateStatus(OrderStatus.EXCHANGED);
            orderRepository.save(order);

            log.info("교환 최종 완료 처리 성공: orderId={}, exchangeId={}", event.getOrderId(), event.getExchangeId());

            // TODO: 필요시 사용자 알림 발송 로직 추가
            // - 새 물품 수령이 완료되어 교환이 최종 완료되었음을 알림

        } catch (Exception e) {
            log.error("Failed to process exchange.completed event: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId(), e);
            throw e;
        }
    }

    /**
     * inventory.decrease Outbox 저장
     *
     * 교환 승인 시 신규 옵션(newOptionId != originalOptionId)에 대한 재고를
     * Product Service에서 차감하도록 이벤트를 발행한다.
     */
    private void saveInventoryDecreaseOutbox(Order order, Long exchangeId,
                                             List<InventoryDecreaseEvent.DecreaseItem> decreaseItems) {
        InventoryDecreaseEvent event = InventoryDecreaseEvent.builder()
                .orderId(order.getId())
                .exchangeId(exchangeId)
                .reason("EXCHANGE_APPROVED")
                .items(decreaseItems)
                .occurredAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(String.valueOf(order.getId()))
                    .eventType(EventTypeConstants.TOPIC_INVENTORY_DECREASE)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("InventoryDecreaseEvent Outbox 저장 완료: orderId={}, exchangeId={}",
                    order.getId(), exchangeId);
        } catch (JsonProcessingException e) {
            log.error("InventoryDecreaseEvent 직렬화 실패: orderId={}, exchangeId={}",
                    order.getId(), exchangeId, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    /**
     * DLT 핸들러
     *
     * 모든 교환 관련 이벤트의 재시도가 실패했을 때 호출된다.
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

        if (payload instanceof ExchangeApprovedEvent event) {
            log.error("DLQ 처리 필요 - exchange.approved 실패: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId());
        } else if (payload instanceof ExchangeCollectingEvent event) {
            log.error("DLQ 처리 필요 - exchange.collecting 실패: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId());
        } else if (payload instanceof ExchangeShippingEvent event) {
            log.error("DLQ 처리 필요 - exchange.shipping 실패: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId());
        } else if (payload instanceof ExchangeReturnCompletedEvent event) {
            log.error("DLQ 처리 필요 - exchange.return-completed 실패: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId());
        } else if (payload instanceof ExchangeCompletedEvent event) {
            log.error("DLQ 처리 필요 - exchange.completed 실패: exchangeId={}, orderId={}",
                    event.getExchangeId(), event.getOrderId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

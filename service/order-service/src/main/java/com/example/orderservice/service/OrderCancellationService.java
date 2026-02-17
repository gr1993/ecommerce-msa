package com.example.orderservice.service;

import com.example.orderservice.domain.entity.DiscountType;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.event.CouponRestoredEvent;
import com.example.orderservice.domain.event.OrderCancelledEvent;
import com.example.orderservice.global.common.EventTypeConstants;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private static final int EXPIRATION_MINUTES = 10;
    private static final String CANCELLATION_REASON_SYSTEM_TIMEOUT = "SYSTEM_TIMEOUT";

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);
        List<Order> expiredOrders = orderRepository.findExpiredOrdersByStatusWithItems(
                OrderStatus.CREATED, expiredBefore);

        if (expiredOrders.isEmpty()) {
            log.debug("취소할 만료된 주문이 없습니다.");
            return;
        }

        log.info("취소할 만료된 주문 {}건을 찾았습니다.", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                cancelOrder(order);
                log.info("주문 자동 취소 완료: orderId={}, orderNumber={}",
                        order.getId(), order.getOrderNumber());
            } catch (Exception e) {
                log.error("주문 자동 취소 실패: orderId={}, orderNumber={}",
                        order.getId(), order.getOrderNumber(), e);
            }
        }
    }

    private void cancelOrder(Order order) {
        order.updateStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        saveOrderCancelledOutbox(order);
        saveCouponRestoredOutboxes(order);
    }

    private void saveCouponRestoredOutboxes(Order order) {
        order.getOrderDiscounts().stream()
                .filter(d -> d.getDiscountType() == DiscountType.COUPON)
                .forEach(discount -> {
                    CouponRestoredEvent event = CouponRestoredEvent.builder()
                            .orderId(order.getOrderNumber())
                            .userCouponId(discount.getReferenceId())
                            .restoredAt(LocalDateTime.now())
                            .build();
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        Outbox outbox = Outbox.builder()
                                .aggregateType("Coupon")
                                .aggregateId(String.valueOf(discount.getReferenceId()))
                                .eventType(EventTypeConstants.TOPIC_COUPON_RESTORED)
                                .payload(payload)
                                .build();
                        outboxRepository.save(outbox);
                        log.debug("CouponRestoredEvent Outbox 저장 완료: orderId={}, userCouponId={}",
                                order.getId(), discount.getReferenceId());
                    } catch (JsonProcessingException e) {
                        log.error("CouponRestoredEvent 직렬화 실패: orderId={}, userCouponId={}",
                                order.getId(), discount.getReferenceId(), e);
                        throw new RuntimeException("이벤트 직렬화 실패", e);
                    }
                });
    }

    private void saveOrderCancelledOutbox(Order order) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .cancellationReason(CANCELLATION_REASON_SYSTEM_TIMEOUT)
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
            log.debug("OrderCancelledEvent Outbox 저장 완료: orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("OrderCancelledEvent 직렬화 실패: orderId={}", order.getId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

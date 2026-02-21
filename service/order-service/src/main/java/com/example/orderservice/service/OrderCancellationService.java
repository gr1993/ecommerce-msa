package com.example.orderservice.service;

import com.example.orderservice.domain.entity.DiscountType;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.event.CouponRestoredEvent;
import com.example.orderservice.domain.event.OrderCancelledEvent;
import com.example.orderservice.client.ShippingServiceClient;
import com.example.orderservice.client.dto.ShippingCancellableResponse;
import com.example.orderservice.dto.response.CancelOrderResponse;
import com.example.orderservice.global.common.EventTypeConstants;
import com.example.orderservice.global.exception.OrderCancelException;
import com.example.orderservice.global.exception.OrderNotFoundException;
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
    private static final String DEFAULT_CANCELLATION_REASON = "고객 요청";

    private static final List<OrderStatus> CANCELLABLE_STATUSES = List.of(
            OrderStatus.CREATED, OrderStatus.PAID
    );

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ShippingServiceClient shippingServiceClient;

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

    @Transactional
    public CancelOrderResponse cancelByUser(Long userId, Long orderId, String cancellationReason) {
        Order order = orderRepository.findByIdWithOrderItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 주문만 취소할 수 있습니다.");
        }

        validateCancellable(order);
        validateShippingCancellable(orderId);

        String reason = (cancellationReason != null && !cancellationReason.isBlank())
                ? cancellationReason : DEFAULT_CANCELLATION_REASON;

        cancelOrder(order, reason);
        log.info("사용자 주문 취소 완료: orderId={}, userId={}", orderId, userId);
        return CancelOrderResponse.of(order, reason);
    }

    @Transactional
    public CancelOrderResponse cancelByAdmin(Long orderId, String cancellationReason) {
        Order order = orderRepository.findByIdWithOrderItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        validateCancellable(order);
        validateShippingCancellable(orderId);

        String reason = (cancellationReason != null && !cancellationReason.isBlank())
                ? cancellationReason : DEFAULT_CANCELLATION_REASON;

        cancelOrder(order, reason);
        log.info("관리자 주문 취소 완료: orderId={}", orderId);
        return CancelOrderResponse.of(order, reason);
    }

    private void validateCancellable(Order order) {
        if (!CANCELLABLE_STATUSES.contains(order.getOrderStatus())) {
            throw new OrderCancelException(
                    "취소할 수 없는 주문 상태입니다. 현재 상태: " + order.getOrderStatus());
        }
    }

    private void validateShippingCancellable(Long orderId) {
        try {
            ShippingCancellableResponse shippingCheck = shippingServiceClient.checkCancellable(orderId);
            if (!shippingCheck.isCancellable()) {
                throw new OrderCancelException(
                        shippingCheck.getReason() != null
                                ? shippingCheck.getReason()
                                : "배송 상태로 인해 주문을 취소할 수 없습니다.");
            }
        } catch (OrderCancelException e) {
            throw e;
        } catch (Exception e) {
            // shipping-service 장애 시 취소 진행 불가 처리 (운영 정책에 따라 조정 가능)
            log.error("shipping-service 배송 취소 가능 여부 확인 실패: orderId={}", orderId, e);
            throw new OrderCancelException("배송 상태 확인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void cancelOrder(Order order, String cancellationReason) {
        order.updateStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        saveOrderCancelledOutbox(order, cancellationReason);
        saveCouponRestoredOutboxes(order);
    }

    private void cancelOrder(Order order) {
        cancelOrder(order, CANCELLATION_REASON_SYSTEM_TIMEOUT);
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
            log.debug("OrderCancelledEvent Outbox 저장 완료: orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("OrderCancelledEvent 직렬화 실패: orderId={}", order.getId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

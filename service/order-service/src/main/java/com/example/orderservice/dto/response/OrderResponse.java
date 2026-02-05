package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private OrderStatus orderStatus;
    private BigDecimal totalProductAmount;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalPaymentAmount;
    private String orderMemo;
    private LocalDateTime orderedAt;
    private List<OrderItemResponse> orderItems;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .totalProductAmount(order.getTotalProductAmount())
                .totalDiscountAmount(order.getTotalDiscountAmount())
                .totalPaymentAmount(order.getTotalPaymentAmount())
                .orderMemo(order.getOrderMemo())
                .orderedAt(order.getOrderedAt())
                .orderItems(order.getOrderItems().stream()
                        .map(OrderItemResponse::from)
                        .toList())
                .build();
    }
}

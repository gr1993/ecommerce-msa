package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "주문 생성 응답")
@Getter
@Builder
public class OrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240101-ABCD1234")
    private String orderNumber;

    @Schema(description = "주문 상태", example = "CREATED")
    private OrderStatus orderStatus;

    @Schema(description = "주문 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime orderedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .orderedAt(order.getOrderedAt())
                .build();
    }
}

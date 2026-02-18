package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 주문 목록 응답")
@Getter
@Builder
public class MyOrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240115-ABCD1234")
    private String orderNumber;

    @Schema(description = "주문 상태", example = "PAID")
    private OrderStatus orderStatus;

    @Schema(description = "결제 총 금액", example = "140000")
    private BigDecimal totalPaymentAmount;

    @Schema(description = "주문 일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;

    @Schema(description = "주문 상품 목록")
    private List<MyOrderItemResponse> items;

    public static MyOrderResponse from(Order order) {
        List<MyOrderItemResponse> items = order.getOrderItems().stream()
                .map(MyOrderItemResponse::from)
                .toList();

        return MyOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .totalPaymentAmount(order.getTotalPaymentAmount())
                .orderedAt(order.getOrderedAt())
                .items(items)
                .build();
    }
}

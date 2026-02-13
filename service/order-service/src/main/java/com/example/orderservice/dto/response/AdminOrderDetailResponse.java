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

@Schema(description = "관리자 주문 상세 응답")
@Getter
@Builder
public class AdminOrderDetailResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20240115-ABCD1234")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "주문 상태", example = "PAID")
    private OrderStatus orderStatus;

    @Schema(description = "상품 총 금액", example = "150000")
    private BigDecimal totalProductAmount;

    @Schema(description = "할인 총 금액", example = "10000")
    private BigDecimal totalDiscountAmount;

    @Schema(description = "결제 총 금액", example = "140000")
    private BigDecimal totalPaymentAmount;

    @Schema(description = "주문 메모")
    private String orderMemo;

    @Schema(description = "주문 일시", example = "2024-01-15 10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderedAt;

    @Schema(description = "수정 일시", example = "2024-01-15 10:35:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "주문 상품 목록")
    private List<AdminOrderItemResponse> orderItems;

    @Schema(description = "배송 정보")
    private AdminOrderShippingResponse orderShipping;

    public static AdminOrderDetailResponse from(Order order) {
        List<AdminOrderItemResponse> items = order.getOrderItems().stream()
                .map(AdminOrderItemResponse::from)
                .toList();

        AdminOrderShippingResponse shipping = order.getOrderDelivery() != null
                ? AdminOrderShippingResponse.from(order.getOrderDelivery(), order.getOrderStatus())
                : null;

        return AdminOrderDetailResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .totalProductAmount(order.getTotalProductAmount())
                .totalDiscountAmount(order.getTotalDiscountAmount())
                .totalPaymentAmount(order.getTotalPaymentAmount())
                .orderMemo(order.getOrderMemo())
                .orderedAt(order.getOrderedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(items)
                .orderShipping(shipping)
                .build();
    }
}

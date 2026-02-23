package com.example.orderservice.dto.response;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 테스트용 결제 완료 주문 생성 응답 DTO
 */
@Schema(description = "테스트용 결제 완료 주문 생성 응답")
@Getter
@Builder
public class TestCreateOrderResponse {

    @Schema(description = "주문 ID", example = "1")
    private Long orderId;

    @Schema(description = "주문 번호", example = "ORD-20250223-0001")
    private String orderNumber;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "주문 상태", example = "PAID")
    private OrderStatus orderStatus;

    @Schema(description = "총 상품 금액", example = "50000")
    private BigDecimal totalProductAmount;

    @Schema(description = "총 할인 금액", example = "0")
    private BigDecimal totalDiscountAmount;

    @Schema(description = "총 결제 금액", example = "50000")
    private BigDecimal totalPaymentAmount;

    @Schema(description = "주문일시")
    private LocalDateTime orderedAt;

    @Schema(description = "배송 서비스 데이터 생성 여부")
    private boolean shippingCreated;

    @Schema(description = "결제 서비스 데이터 생성 여부")
    private boolean paymentCreated;

    public static TestCreateOrderResponse from(Order order, boolean shippingCreated, boolean paymentCreated) {
        return TestCreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus())
                .totalProductAmount(order.getTotalProductAmount())
                .totalDiscountAmount(order.getTotalDiscountAmount())
                .totalPaymentAmount(order.getTotalPaymentAmount())
                .orderedAt(order.getOrderedAt())
                .shippingCreated(shippingCreated)
                .paymentCreated(paymentCreated)
                .build();
    }
}

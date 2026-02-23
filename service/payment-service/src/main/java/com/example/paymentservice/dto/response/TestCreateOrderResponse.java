package com.example.paymentservice.dto.response;

import com.example.paymentservice.domain.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 테스트용 결제 완료 주문 생성 응답 DTO
 */
@Schema(description = "테스트용 결제 완료 주문 생성 응답")
@Getter
@Builder
public class TestCreateOrderResponse {

    @Schema(description = "MongoDB 문서 ID", example = "507f1f77bcf86cd799439011")
    private String id;

    @Schema(description = "주문 번호", example = "ORD-20250223-0001")
    private String orderNumber;

    @Schema(description = "주문명", example = "테스트 상품 외 2개")
    private String orderName;

    @Schema(description = "결제 금액", example = "50000")
    private Long amount;

    @Schema(description = "고객 ID", example = "1")
    private String customerId;

    @Schema(description = "결제 키", example = "tgen_test_abcd1234efgh5678")
    private String paymentKey;

    @Schema(description = "결제 상태", example = "APPROVED")
    private Order.PaymentStatus status;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "결제 승인일시")
    private LocalDateTime approvedAt;

    public static TestCreateOrderResponse from(Order order) {
        return TestCreateOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderName(order.getOrderName())
                .amount(order.getAmount())
                .customerId(order.getCustomerId())
                .paymentKey(order.getPaymentKey())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .approvedAt(order.getApprovedAt())
                .build();
    }
}

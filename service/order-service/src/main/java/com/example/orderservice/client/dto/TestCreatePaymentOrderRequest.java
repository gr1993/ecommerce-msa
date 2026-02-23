package com.example.orderservice.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * payment-service 테스트용 결제 완료 주문 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
public class TestCreatePaymentOrderRequest {

    private String orderNumber;
    private Long userId;
    private BigDecimal totalPaymentAmount;
    private List<OrderItemInfo> orderItems;
    private String paymentKey;

    @Builder
    public TestCreatePaymentOrderRequest(String orderNumber, Long userId, BigDecimal totalPaymentAmount,
                                          List<OrderItemInfo> orderItems, String paymentKey) {
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.totalPaymentAmount = totalPaymentAmount;
        this.orderItems = orderItems;
        this.paymentKey = paymentKey;
    }

    @Getter
    @NoArgsConstructor
    public static class OrderItemInfo {
        private String productName;
        private Integer quantity;

        @Builder
        public OrderItemInfo(String productName, Integer quantity) {
            this.productName = productName;
            this.quantity = quantity;
        }
    }
}

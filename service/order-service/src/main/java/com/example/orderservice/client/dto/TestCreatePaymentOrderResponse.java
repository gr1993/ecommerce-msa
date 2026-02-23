package com.example.orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * payment-service 테스트용 결제 완료 주문 생성 응답 DTO
 */
@Getter
@NoArgsConstructor
public class TestCreatePaymentOrderResponse {

    private String id;
    private String orderNumber;
    private String orderName;
    private Long amount;
    private String customerId;
    private String paymentKey;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}

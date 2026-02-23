package com.example.orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * shipping-service 테스트용 배송 정보 생성 응답 DTO
 */
@Getter
@NoArgsConstructor
public class TestCreateShippingResponse {

    private Long shippingId;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String shippingStatus;
    private String deliveryServiceStatus;
    private LocalDateTime createdAt;
}

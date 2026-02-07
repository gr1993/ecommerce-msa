package com.example.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmResponse {
    private String orderId;
    private String paymentKey;
    private Long amount;
    private String status;
    private String approvedAt;
}

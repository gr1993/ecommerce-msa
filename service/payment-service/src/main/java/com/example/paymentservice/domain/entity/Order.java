package com.example.paymentservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String orderId;
    private String orderName;
    private Long amount;
    private String paymentKey;
    private PaymentStatus status;
    private String customerId;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public void approve(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public enum PaymentStatus {
        PENDING,    // 결제 대기
        APPROVED,   // 결제 승인
        FAILED,     // 결제 실패
        CANCELED    // 결제 취소
    }
}

package com.example.promotionservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsedEvent {

    private String orderId;
    private Long userCouponId;
    private String customerId;
    private LocalDateTime usedAt;
}
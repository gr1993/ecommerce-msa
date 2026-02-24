package com.example.orderservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 반품 승인 이벤트
 *
 * Shipping Service에서 관리자가 반품을 승인할 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnApprovedEvent {

    private Long returnId;
    private Long orderId;
    private Long userId;
    private String courier;
    private String trackingNumber;
    private LocalDateTime approvedAt;
}

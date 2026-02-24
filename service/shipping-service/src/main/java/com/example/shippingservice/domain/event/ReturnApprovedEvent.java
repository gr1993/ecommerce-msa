package com.example.shippingservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 반품 승인 이벤트
 *
 * 관리자가 반품을 승인하고 택배사 회수 운송장이 발급될 때 발행되는 이벤트.
 * Order Service에서 이 이벤트를 소비하여 필요한 처리를 수행한다.
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

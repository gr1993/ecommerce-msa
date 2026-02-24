package com.example.orderservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 배송 완료 이벤트
 *
 * Shipping Service에서 배송이 완료될 때 발행되는 이벤트.
 * Order Service에서 이 이벤트를 소비하여 주문 상태를 DELIVERED로 변경한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingDeliveredEvent {

    private Long shippingId;
    private Long orderId;
    private String trackingNumber;
    private LocalDateTime deliveredAt;
}

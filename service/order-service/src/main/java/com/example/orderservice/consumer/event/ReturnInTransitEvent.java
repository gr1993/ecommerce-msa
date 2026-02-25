package com.example.orderservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 반품 수거 중 이벤트
 *
 * Shipping Service에서 반품 물품이 택배사에 의해 수거되어 운송 중일 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnInTransitEvent {

    private Long returnId;
    private Long orderId;
    private Long userId;
    private String courier;
    private String trackingNumber;
    private LocalDateTime inTransitAt;
}

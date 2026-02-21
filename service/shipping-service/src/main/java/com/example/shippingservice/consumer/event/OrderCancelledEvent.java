package com.example.shippingservice.consumer.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * order.cancelled 이벤트 DTO
 * order-service가 발행하며, shipping-service에서 배송 취소 처리를 위해 소비합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    private Long orderId;
    private String orderNumber;
    private String cancellationReason;
    private Long userId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime cancelledAt;
}

package com.example.orderservice.consumer.event;

import com.example.orderservice.client.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 승인 이벤트
 *
 * Shipping Service에서 관리자가 교환을 승인하고 회수/발송 운송장을 발급했을 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeApprovedEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private List<ExchangeItemDto> exchangeItems;
    private String collectCourier;
    private String collectTrackingNumber;
    private LocalDateTime approvedAt;
}

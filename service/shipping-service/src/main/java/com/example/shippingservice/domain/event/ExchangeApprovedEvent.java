package com.example.shippingservice.domain.event;

import com.example.shippingservice.exchange.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 승인 이벤트
 *
 * 관리자가 교환 요청을 승인하고 회수 운송장이 발급될 때 발행되는 이벤트.
 * Order Service에서 이 이벤트를 소비하여 재고 차감 등 필요한 처리를 수행한다.
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

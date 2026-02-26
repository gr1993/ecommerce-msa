package com.example.orderservice.consumer.event;

import com.example.orderservice.client.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 물품 회수 완료 이벤트
 *
 * Shipping Service에서 기존 물품이 창고에 도착하여 검수 대기 상태가 되었을 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeReturnCompletedEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private List<ExchangeItemDto> exchangeItems;
    private String courier;
    private String trackingNumber;
    private LocalDateTime returnCompletedAt;
}

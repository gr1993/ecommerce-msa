package com.example.orderservice.consumer.event;

import com.example.orderservice.client.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 최종 완료 이벤트
 *
 * Shipping Service에서 고객이 새 물품을 수령하여 교환이 최종 완료되었을 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeCompletedEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private List<ExchangeItemDto> exchangeItems;
    private String reason;
    private LocalDateTime completedAt;
}

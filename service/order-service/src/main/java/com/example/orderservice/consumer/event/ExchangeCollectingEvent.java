package com.example.orderservice.consumer.event;

import com.example.orderservice.client.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 물품 회수 중 이벤트
 *
 * Shipping Service에서 택배사가 기존 물품을 수거하여 이동 중일 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeCollectingEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private List<ExchangeItemDto> exchangeItems;
    private String courier;
    private String trackingNumber;
    private LocalDateTime collectingAt;
}

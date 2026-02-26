package com.example.orderservice.consumer.event;

import com.example.orderservice.client.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 교환 새 물품 배송 중 이벤트
 *
 * Shipping Service에서 교환할 새 물품이 배송을 시작했을 때 발행되는 이벤트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeShippingEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private List<ExchangeItemDto> exchangeItems;
    private String courier;
    private String trackingNumber;
    private LocalDateTime shippingAt;
}

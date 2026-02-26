package com.example.shippingservice.domain.event;

import com.example.shippingservice.exchange.dto.ExchangeItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

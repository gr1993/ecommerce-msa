package com.example.shippingservice.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeCollectingEvent {

    private Long exchangeId;
    private Long orderId;
    private Long userId;
    private String courier;
    private String trackingNumber;
    private LocalDateTime collectingAt;
}

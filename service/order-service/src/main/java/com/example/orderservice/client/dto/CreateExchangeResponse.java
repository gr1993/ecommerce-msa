package com.example.orderservice.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class CreateExchangeResponse {

    private Long exchangeId;
    private Long orderId;
    private List<ExchangeItemDto> exchangeItems;
    private String exchangeStatus;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;
}

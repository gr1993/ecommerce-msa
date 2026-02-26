package com.example.shippingservice.exchange.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeItemDto {

    private Long orderItemId;
    private Long originalOptionId;
    private Long newOptionId;
    private Integer quantity;
}

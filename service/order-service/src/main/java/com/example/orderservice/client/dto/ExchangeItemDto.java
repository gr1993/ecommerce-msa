package com.example.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeItemDto {

    private Long orderItemId;
    private Long originalOptionId;
    private Long newOptionId;
    private Integer quantity;
}

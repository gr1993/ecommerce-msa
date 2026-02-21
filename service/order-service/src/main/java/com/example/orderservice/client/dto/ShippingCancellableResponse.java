package com.example.orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShippingCancellableResponse {

    private boolean cancellable;
    private String reason;
    private String shippingStatus;
}

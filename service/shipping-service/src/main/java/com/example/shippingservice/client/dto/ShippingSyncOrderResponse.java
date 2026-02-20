package com.example.shippingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingSyncOrderResponse {

    private Long orderId;
    private String orderNumber;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String postalCode;
    private String deliveryMemo;
    private LocalDateTime orderedAt;
}

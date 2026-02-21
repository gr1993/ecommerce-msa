package com.example.orderservice.client;

import com.example.orderservice.client.dto.ShippingCancellableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "shipping-service", url = "${shipping-service.url}")
public interface ShippingServiceClient {

    @GetMapping("/internal/shipping/orders/{orderId}/cancellable")
    ShippingCancellableResponse checkCancellable(@PathVariable("orderId") Long orderId);
}

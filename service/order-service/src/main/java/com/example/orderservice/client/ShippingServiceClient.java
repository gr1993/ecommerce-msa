package com.example.orderservice.client;

import com.example.orderservice.client.dto.CreateExchangeRequest;
import com.example.orderservice.client.dto.CreateExchangeResponse;
import com.example.orderservice.client.dto.CreateReturnRequest;
import com.example.orderservice.client.dto.CreateReturnResponse;
import com.example.orderservice.client.dto.ShippingCancellableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "shipping-service", url = "${shipping-service.url}")
public interface ShippingServiceClient {

    @GetMapping("/internal/shipping/orders/{orderId}/cancellable")
    ShippingCancellableResponse checkCancellable(@PathVariable("orderId") Long orderId);

    @PostMapping("/internal/shipping/returns")
    CreateReturnResponse createReturn(@RequestBody CreateReturnRequest request);

    @PostMapping("/internal/shipping/exchanges")
    CreateExchangeResponse createExchange(@RequestBody CreateExchangeRequest request);
}

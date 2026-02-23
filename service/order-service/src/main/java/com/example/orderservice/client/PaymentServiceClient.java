package com.example.orderservice.client;

import com.example.orderservice.client.dto.TestCreatePaymentOrderRequest;
import com.example.orderservice.client.dto.TestCreatePaymentOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${payment-service.url}")
public interface PaymentServiceClient {

    @PostMapping("/internal/payments/test/orders")
    TestCreatePaymentOrderResponse createOrderForTest(@RequestBody TestCreatePaymentOrderRequest request);

    @DeleteMapping("/internal/payments/test/orders/{orderNumber}")
    void deleteOrderForTest(@PathVariable("orderNumber") String orderNumber);
}

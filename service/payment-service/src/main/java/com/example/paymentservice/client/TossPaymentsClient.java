package com.example.paymentservice.client;

import com.example.paymentservice.dto.request.TossPaymentConfirmRequest;
import com.example.paymentservice.dto.response.TossPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "toss-payments",
    url = "${toss.payments.api.base-url}",
    configuration = TossPaymentsConfig.class
)
public interface TossPaymentsClient {

    @PostMapping("/v1/payments/confirm")
    TossPaymentResponse confirmPayment(@RequestBody TossPaymentConfirmRequest request);
}

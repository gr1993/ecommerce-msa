package com.example.paymentservice.client;

import com.example.paymentservice.client.dto.TossPaymentCancelRequest;
import com.example.paymentservice.client.dto.TossPaymentConfirmRequest;
import com.example.paymentservice.client.dto.TossPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/v1/payments/{paymentKey}/cancel")
    TossPaymentResponse cancelPayment(
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody TossPaymentCancelRequest request
    );
}

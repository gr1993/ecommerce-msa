package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentConfirmRequest;
import com.example.paymentservice.dto.PaymentConfirmResponse;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인 API
     * 클라이언트가 successUrl로 리다이렉트되면서 전달받은 파라미터로 결제 승인 요청
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @RequestBody PaymentConfirmRequest request) {

        log.info("결제 승인 요청 수신 - orderId: {}", request.getOrderId());

        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        return ResponseEntity.ok(response);
    }
}

package com.example.paymentservice.service;

import com.example.paymentservice.client.TossPaymentsClient;
import com.example.paymentservice.domain.entity.Order;
import com.example.paymentservice.domain.entity.Outbox;
import com.example.paymentservice.domain.event.PaymentCancelledEvent;
import com.example.paymentservice.domain.event.PaymentConfirmedEvent;
import com.example.paymentservice.dto.request.PaymentConfirmRequest;
import com.example.paymentservice.client.dto.TossPaymentConfirmRequest;
import com.example.paymentservice.client.dto.TossPaymentResponse;
import com.example.paymentservice.dto.response.PaymentConfirmResponse;
import com.example.paymentservice.global.common.EventTypeConstants;
import com.example.paymentservice.repository.OrderRepository;
import com.example.paymentservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TossPaymentsClient tossPaymentsClient;

    /**
     * 결제 승인 처리
     * 1. 주문 정보 조회
     * 2. 결제 금액과 주문 금액 검증 (실패 시 Order FAILED + Outbox 저장)
     * 3. 토스페이먼츠 결제 승인 API 호출
     * 4. 주문 상태 업데이트
     */
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        log.info("결제 승인 요청 - orderId: {}, paymentKey: {}, amount: {}",
                request.getOrderId(), request.getPaymentKey(), request.getAmount());

        // 1. 주문 정보 조회
        Order order = orderRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. orderId: " + request.getOrderId()));

        // 2. 결제 금액과 주문 금액 검증 (위변조 방지)
        if (!order.getAmount().equals(request.getAmount())) {
            log.error("결제 금액 불일치 - 주문 금액: {}, 요청 금액: {}", order.getAmount(), request.getAmount());
            order.fail();
            orderRepository.save(order);
            savePaymentCancelledOutbox(order, "결제 금액 불일치");
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 3. 토스페이먼츠 결제 승인 API 호출
        TossPaymentConfirmRequest tossRequest = TossPaymentConfirmRequest.builder()
                .paymentKey(request.getPaymentKey())
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .build();

        TossPaymentResponse tossResponse = tossPaymentsClient.confirmPayment(tossRequest);

        log.info("토스페이먼츠 결제 승인 완료 - orderId: {}, status: {}",
                tossResponse.getOrderId(), tossResponse.getStatus());

        // 4. 주문 상태 업데이트
        order.approve(request.getPaymentKey());
        orderRepository.save(order);

        // 5. 결제 완료 이벤트 Outbox 저장
        savePaymentConfirmedOutbox(order, tossResponse);

        return PaymentConfirmResponse.builder()
                .orderId(tossResponse.getOrderId())
                .paymentKey(tossResponse.getPaymentKey())
                .amount(tossResponse.getTotalAmount())
                .status(tossResponse.getStatus())
                .approvedAt(tossResponse.getApprovedAt())
                .build();
    }

    private void savePaymentConfirmedOutbox(Order order, TossPaymentResponse tossResponse) {
        PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                .orderId(Long.parseLong(order.getOrderId()))
                .paymentKey(tossResponse.getPaymentKey())
                .paymentMethod(tossResponse.getMethod())
                .paymentAmount(tossResponse.getTotalAmount())
                .paymentStatus("PAID")
                .paidAt(tossResponse.getApprovedAt())
                .customerId(order.getCustomerId())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getOrderId())
                    .eventType(EventTypeConstants.TOPIC_PAYMENT_CONFIRMED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.info("결제 완료 Outbox 이벤트 저장 완료 - orderId: {}", order.getOrderId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    private void savePaymentCancelledOutbox(Order order, String cancelReason) {
        PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                .orderId(order.getOrderId())
                .amount(order.getAmount())
                .customerId(order.getCustomerId())
                .cancelReason(cancelReason)
                .cancelledAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getOrderId())
                    .eventType(EventTypeConstants.TOPIC_PAYMENT_CANCELLED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.info("결제 취소 Outbox 이벤트 저장 완료 - orderId: {}, reason: {}", order.getOrderId(), cancelReason);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

package com.example.paymentservice.controller;

import com.example.paymentservice.domain.entity.Order;
import com.example.paymentservice.dto.request.TestCreateOrderRequest;
import com.example.paymentservice.dto.response.TestCreateOrderResponse;
import com.example.paymentservice.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 내부 서비스용 결제 API
 * 테스트 데이터 생성 및 삭제를 위한 엔드포인트를 제공합니다.
 */
@Tag(name = "Internal Payment", description = "내부 서비스용 결제 API (테스트용)")
@Slf4j
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final OrderRepository orderRepository;

    @Operation(
            summary = "[테스트] 결제 완료 주문 생성",
            description = "테스트용 결제 완료 주문 생성 API입니다. order.created 이벤트 수신 후 결제 확정까지 "
                    + "완료된 상태(APPROVED)로 생성합니다. 동일한 orderNumber로 재요청해도 기존 데이터를 반환하여 멱등성을 보장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "주문 생성 성공")
            }
    )
    @PostMapping("/test/orders")
    public ResponseEntity<TestCreateOrderResponse> createOrderForTest(
            @Valid @RequestBody TestCreateOrderRequest request) {

        log.info("[TEST] 결제 완료 주문 생성 요청: orderNumber={}", request.getOrderNumber());

        // 멱등성 보장: 이미 존재하는 경우 기존 데이터 반환
        var existing = orderRepository.findByOrderNumber(request.getOrderNumber());
        if (existing.isPresent()) {
            log.info("[TEST] 이미 존재하는 주문 반환: orderNumber={}", request.getOrderNumber());
            return ResponseEntity.ok(TestCreateOrderResponse.from(existing.get()));
        }

        // paymentKey가 없으면 테스트용 키 자동 생성
        String paymentKey = request.getPaymentKey();
        if (paymentKey == null || paymentKey.isBlank()) {
            paymentKey = "tgen_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        }

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .orderNumber(request.getOrderNumber())
                .orderName(request.generateOrderName())
                .amount(request.getTotalPaymentAmount().longValue())
                .customerId(request.getUserId().toString())
                .paymentKey(paymentKey)
                .status(Order.PaymentStatus.APPROVED)
                .createdAt(now)
                .approvedAt(now)
                .build();

        Order saved = orderRepository.save(order);
        log.info("[TEST] 결제 완료 주문 생성 완료: orderNumber={}, id={}, paymentKey={}",
                saved.getOrderNumber(), saved.getId(), saved.getPaymentKey());

        return ResponseEntity.ok(TestCreateOrderResponse.from(saved));
    }

    @Operation(
            summary = "[테스트] 결제 주문 삭제",
            description = "테스트 데이터 정리를 위한 주문 삭제 API입니다. "
                    + "해당 주문 번호의 데이터가 없어도 오류를 발생시키지 않습니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공")
            }
    )
    @DeleteMapping("/test/orders/{orderNumber}")
    public ResponseEntity<Void> deleteOrderForTest(
            @Parameter(description = "주문 번호", required = true, example = "ORD-20250223-0001")
            @PathVariable String orderNumber) {

        log.info("[TEST] 결제 주문 삭제 요청: orderNumber={}", orderNumber);

        orderRepository.findByOrderNumber(orderNumber).ifPresent(order -> {
            orderRepository.delete(order);
            log.info("[TEST] 결제 주문 삭제 완료: orderNumber={}, id={}", orderNumber, order.getId());
        });

        return ResponseEntity.noContent().build();
    }
}

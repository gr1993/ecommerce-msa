package com.example.productservice.consumer.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 취소 이벤트 (보상 트랜잭션)
 *
 * 발생 시나리오:
 * 1. 주문 생성 시 재고 선차감 완료 (order.created)
 * 2. 결제 시간 초과 또는 결제 실패
 * 3. Payment Service에서 payment.cancelled 이벤트 발행
 * 4. Product Service가 구독하여 차감했던 재고를 복구 (보상 트랜잭션)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelledEvent {

    private Long orderId;
    private String orderNumber;
    private Long paymentId;
    private String cancellationReason;  // PAYMENT_TIMEOUT, PAYMENT_FAILED, INSUFFICIENT_BALANCE 등
    private Long userId;
    private List<PaymentItem> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime cancelledAt;

    /**
     * 결제 취소된 항목 (재고 복구 대상)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItem {
        private Long orderItemId;
        private Long productId;
        private Long skuId;
        private String productName;
        private String productCode;
        private Integer quantity;  // 복구할 재고 수량
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

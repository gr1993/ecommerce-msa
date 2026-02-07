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
 * 주문 취소 이벤트 (보상 트랜잭션)
 *
 * 발생 시나리오:
 * 1. 주문 생성 시 재고 선차감 완료 (order.created)
 * 2. 사용자가 주문 취소 요청
 * 3. Order Service에서 order.cancelled 이벤트 발행
 * 4. Product Service가 구독하여 차감했던 재고를 복구 (보상 트랜잭션)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    private Long orderId;
    private String orderNumber;
    private String cancellationReason;  // USER_REQUEST, ADMIN_CANCEL, SYSTEM_TIMEOUT 등
    private Long userId;
    private List<CancelledOrderItem> cancelledItems;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime cancelledAt;

    /**
     * 취소된 주문 항목 (재고 복구 대상)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelledOrderItem {
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

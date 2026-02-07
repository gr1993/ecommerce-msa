package com.example.productservice.product.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 부족 이벤트 (발행용)
 *
 * 발행 시나리오:
 * 1. Product Service가 order.created 이벤트 수신
 * 2. 재고 확인 시 재고 부족 발견
 * 3. 재고 차감 없이 stock.rejected 이벤트 발행
 * 4. Order Service와 Payment Service가 구독하여 보상 트랜잭션 수행
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRejectedEvent {

    private Long orderId;
    private String orderNumber;
    private String rejectionReason;  // INSUFFICIENT_STOCK
    private Long userId;
    private List<RejectedItem> rejectedItems;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime rejectedAt;

    /**
     * 재고 부족으로 거부된 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedItem {
        private Long orderItemId;
        private Long productId;
        private Long skuId;
        private String productName;
        private String productCode;
        private Integer requestedQuantity;  // 요청한 수량
        private Integer availableStock;     // 현재 가용 재고
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

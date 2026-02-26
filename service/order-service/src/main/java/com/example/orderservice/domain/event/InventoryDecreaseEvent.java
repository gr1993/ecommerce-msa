package com.example.orderservice.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 차감 이벤트
 *
 * 교환 승인 시 신규 옵션(newSkuId != originalSkuId)에 대해 재고를 차감하기 위해 발행.
 * Product Service가 구독하여 여러 SKU의 재고를 한 번에 처리한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDecreaseEvent {

    private Long orderId;
    private String orderNumber;
    private Long exchangeId;
    private String reason;  // EXCHANGE_APPROVED 등
    private List<DecreaseItem> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime occurredAt;

    /**
     * 차감 대상 재고 항목 (SKU 단위)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecreaseItem {
        private Long skuId;       // 차감할 신규 SKU ID (newOptionId)
        private Integer quantity; // 차감 수량
    }
}

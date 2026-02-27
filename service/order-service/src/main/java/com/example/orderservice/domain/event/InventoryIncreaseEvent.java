package com.example.orderservice.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 증가 이벤트
 *
 * 교환 회수 완료 시 반환된 원래 옵션(originalSkuId)의 재고를 복구하기 위해 발행.
 * newOptionId != originalOptionId 인 경우에만 발행 (동일 옵션 교환은 승인 시 차감이 없었으므로 증가도 없음).
 * Product Service가 구독하여 여러 SKU의 재고를 한 번에 처리한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryIncreaseEvent {

    private Long orderId;
    private String orderNumber;
    private Long exchangeId;
    private String reason;  // EXCHANGE_RETURN_COMPLETED 등
    private List<IncreaseItem> items;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime occurredAt;

    /**
     * 증가 대상 재고 항목 (SKU 단위)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncreaseItem {
        private Long skuId;       // 증가할 원래 SKU ID (originalOptionId)
        private Integer quantity; // 증가 수량
    }
}

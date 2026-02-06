package com.example.productservice.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SKU 재고 변동 이력 엔티티
 */
@Entity
@Table(name = "product_sku_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @Column(name = "order_id", length = 50)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private StockChangeType changeType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "result_stock_qty", nullable = false)
    private Integer resultStockQty;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 재고 차감 이력 생성 (주문)
     */
    public static ProductSkuHistory ofDeduction(ProductSku sku, String orderId, int quantity, int resultQty) {
        return ProductSkuHistory.builder()
                .sku(sku)
                .orderId(orderId)
                .changeType(StockChangeType.DEDUCTION)
                .amount(-quantity)
                .resultStockQty(resultQty)
                .build();
    }

    /**
     * 재고 복구 이력 생성 (주문 취소)
     */
    public static ProductSkuHistory ofRestore(ProductSku sku, String orderId, int quantity, int resultQty, String reason) {
        return ProductSkuHistory.builder()
                .sku(sku)
                .orderId(orderId)
                .changeType(StockChangeType.RESTORE)
                .amount(quantity)
                .resultStockQty(resultQty)
                .reason(reason)
                .build();
    }

    /**
     * 초기 재고 이력 생성 (상품 등록)
     */
    public static ProductSkuHistory ofInitial(ProductSku sku, int initialQty) {
        return ProductSkuHistory.builder()
                .sku(sku)
                .changeType(StockChangeType.INITIAL)
                .amount(initialQty)
                .resultStockQty(initialQty)
                .reason("상품 등록 시 초기 재고 설정")
                .build();
    }

    /**
     * 수동 수정 이력 생성 (관리자)
     */
    public static ProductSkuHistory ofManualEdit(ProductSku sku, int previousQty, int newQty) {
        int change = newQty - previousQty;
        return ProductSkuHistory.builder()
                .sku(sku)
                .changeType(StockChangeType.MANUAL_EDIT)
                .amount(change)
                .resultStockQty(newQty)
                .reason("관리자 수동 수정")
                .build();
    }
}

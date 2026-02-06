package com.example.productservice.product.service;

import com.example.productservice.product.domain.ProductSku;

import java.util.List;

/**
 * SKU 재고 변동 이력 서비스
 */
public interface ProductSkuHistoryService {

    /**
     * 주문으로 인한 재고 차감 이력 기록
     */
    void recordDeduction(ProductSku sku, String orderId, int quantity, int resultQty);

    /**
     * 주문 취소로 인한 재고 복구 이력 기록
     */
    void recordRestore(ProductSku sku, String orderId, int quantity, int resultQty, String reason);

    /**
     * 상품 등록 시 초기 재고 이력 기록
     */
    void recordInitial(ProductSku sku, int initialQty);

    /**
     * 관리자 수동 수정 이력 기록
     */
    void recordManualEdit(ProductSku sku, int previousQty, int newQty);

    /**
     * 여러 SKU의 초기 재고 이력 일괄 기록
     */
    void recordInitialBatch(List<ProductSku> skus);

    /**
     * 재고 변동이 있는 SKU들의 수동 수정 이력 일괄 기록
     */
    void recordManualEditBatch(List<ProductSku> skus, java.util.Map<Long, Integer> previousStockMap);
}

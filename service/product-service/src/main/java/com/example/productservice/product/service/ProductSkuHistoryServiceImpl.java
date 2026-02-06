package com.example.productservice.product.service;

import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.domain.ProductSkuHistory;
import com.example.productservice.product.repository.ProductSkuHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductSkuHistoryServiceImpl implements ProductSkuHistoryService {

    private final ProductSkuHistoryRepository productSkuHistoryRepository;

    @Override
    public void recordDeduction(ProductSku sku, String orderId, int quantity, int resultQty) {
        ProductSkuHistory history = ProductSkuHistory.ofDeduction(sku, orderId, quantity, resultQty);
        productSkuHistoryRepository.save(history);
        log.info("재고 차감 이력 기록: skuId={}, orderId={}, quantity={}, resultQty={}",
                sku.getSkuId(), orderId, quantity, resultQty);
    }

    @Override
    public void recordRestore(ProductSku sku, String orderId, int quantity, int resultQty, String reason) {
        ProductSkuHistory history = ProductSkuHistory.ofRestore(sku, orderId, quantity, resultQty, reason);
        productSkuHistoryRepository.save(history);
        log.info("재고 복구 이력 기록: skuId={}, orderId={}, quantity={}, resultQty={}",
                sku.getSkuId(), orderId, quantity, resultQty);
    }

    @Override
    public void recordInitial(ProductSku sku, int initialQty) {
        ProductSkuHistory history = ProductSkuHistory.ofInitial(sku, initialQty);
        productSkuHistoryRepository.save(history);
        log.info("초기 재고 이력 기록: skuId={}, initialQty={}", sku.getSkuId(), initialQty);
    }

    @Override
    public void recordManualEdit(ProductSku sku, int previousQty, int newQty) {
        if (previousQty == newQty) {
            return;
        }
        ProductSkuHistory history = ProductSkuHistory.ofManualEdit(sku, previousQty, newQty);
        productSkuHistoryRepository.save(history);
        log.info("수동 수정 이력 기록: skuId={}, previousQty={}, newQty={}, change={}",
                sku.getSkuId(), previousQty, newQty, newQty - previousQty);
    }

    @Override
    public void recordInitialBatch(List<ProductSku> skus) {
        List<ProductSkuHistory> histories = new ArrayList<>();
        for (ProductSku sku : skus) {
            histories.add(ProductSkuHistory.ofInitial(sku, sku.getStockQty()));
        }
        productSkuHistoryRepository.saveAll(histories);
        log.info("초기 재고 이력 일괄 기록: {} SKUs", skus.size());
    }

    @Override
    public void recordManualEditBatch(List<ProductSku> skus, Map<Long, Integer> previousStockMap) {
        List<ProductSkuHistory> histories = new ArrayList<>();
        for (ProductSku sku : skus) {
            Integer previousQty = previousStockMap.get(sku.getSkuId());
            if (previousQty != null && !previousQty.equals(sku.getStockQty())) {
                histories.add(ProductSkuHistory.ofManualEdit(sku, previousQty, sku.getStockQty()));
            }
        }
        if (!histories.isEmpty()) {
            productSkuHistoryRepository.saveAll(histories);
            log.info("수동 수정 이력 일괄 기록: {} SKUs", histories.size());
        }
    }
}

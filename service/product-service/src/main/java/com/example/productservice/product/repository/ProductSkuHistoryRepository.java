package com.example.productservice.product.repository;

import com.example.productservice.product.domain.ProductSkuHistory;
import com.example.productservice.product.domain.StockChangeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSkuHistoryRepository extends JpaRepository<ProductSkuHistory, Long> {

    List<ProductSkuHistory> findBySkuSkuIdOrderByCreatedAtDesc(Long skuId);

    List<ProductSkuHistory> findByOrderId(String orderId);

    List<ProductSkuHistory> findBySkuSkuIdAndChangeType(Long skuId, StockChangeType changeType);
}

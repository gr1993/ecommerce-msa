package com.example.productservice.product.repository;

import com.example.productservice.product.domain.ProductSkuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSkuOptionRepository extends JpaRepository<ProductSkuOption, Long> {

    List<ProductSkuOption> findBySku_SkuId(Long skuId);
}

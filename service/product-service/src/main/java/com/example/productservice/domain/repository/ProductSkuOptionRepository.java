package com.example.productservice.domain.repository;

import com.example.productservice.domain.entity.ProductSkuOption;
import com.example.productservice.domain.entity.ProductSkuOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSkuOptionRepository extends JpaRepository<ProductSkuOption, ProductSkuOptionId> {

    List<ProductSkuOption> findBySku_SkuId(Long skuId);
}

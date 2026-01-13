package com.example.productservice.domain.repository;

import com.example.productservice.domain.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findBySkuCode(String skuCode);

    List<ProductSku> findByProduct_ProductId(Long productId);
}

package com.example.productservice.product.repository;

import com.example.productservice.product.domain.ProductOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionGroupRepository extends JpaRepository<ProductOptionGroup, Long> {

    List<ProductOptionGroup> findByProduct_ProductId(Long productId);
}

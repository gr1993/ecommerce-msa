package com.example.productservice.domain.repository;

import com.example.productservice.domain.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct_ProductId(Long productId);

    Optional<ProductImage> findByProduct_ProductIdAndIsPrimaryTrue(Long productId);
}

package com.example.productservice.product.repository;

import com.example.productservice.product.domain.ProductSearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSearchKeywordRepository extends JpaRepository<ProductSearchKeyword, Long> {

    List<ProductSearchKeyword> findByProductProductIdOrderByCreatedAtDesc(Long productId);

    List<ProductSearchKeyword> findByProductProductIdIn(List<Long> productIds);

    Optional<ProductSearchKeyword> findByProductProductIdAndKeyword(Long productId, String keyword);

    boolean existsByProductProductIdAndKeyword(Long productId, String keyword);

    void deleteByProductProductIdAndKeywordId(Long productId, Long keywordId);
}

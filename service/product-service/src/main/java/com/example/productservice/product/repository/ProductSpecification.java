package com.example.productservice.product.repository;

import com.example.productservice.category.domain.Category;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.dto.ProductSearchRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    public static Specification<Product> searchWith(ProductSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 상품명 검색 (부분 일치)
            if (request.getProductName() != null && !request.getProductName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        root.get("productName"),
                        "%" + request.getProductName().trim() + "%"
                ));
            }

            // 상품 코드 검색 (정확한 일치)
            if (request.getProductCode() != null && !request.getProductCode().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("productCode"),
                        request.getProductCode().trim()
                ));
            }

            // 상태 검색
            if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        request.getStatus().trim()
                ));
            }

            // 진열 여부 검색
            if (request.getIsDisplayed() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("isDisplayed"),
                        request.getIsDisplayed()
                ));
            }

            // 최소 가격 검색
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("basePrice"),
                        BigDecimal.valueOf(request.getMinPrice())
                ));
            }

            // 최대 가격 검색
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("basePrice"),
                        BigDecimal.valueOf(request.getMaxPrice())
                ));
            }

            // 카테고리 검색
            if (request.getCategoryId() != null) {
                Join<Product, Category> categoryJoin = root.join("categories");
                predicates.add(criteriaBuilder.equal(
                        categoryJoin.get("categoryId"),
                        request.getCategoryId()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.example.productservice.product.repository;

import com.example.productservice.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductCode(String productCode);

    @Query(value = "SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.categories " +
            "WHERE p.status = 'ACTIVE' AND p.isDisplayed = true",
            countQuery = "SELECT COUNT(DISTINCT p) FROM Product p WHERE p.status = 'ACTIVE' AND p.isDisplayed = true")
    Page<Product> findActiveDisplayedProductsWithDetails(Pageable pageable);
}

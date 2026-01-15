package com.example.productservice.product.repository;

import com.example.productservice.product.domain.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOptionValueRepository extends JpaRepository<ProductOptionValue, Long> {

    List<ProductOptionValue> findByOptionGroup_OptionGroupId(Long optionGroupId);
}

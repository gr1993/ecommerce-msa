package com.example.productservice.category.repository;

import com.example.productservice.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByDisplayOrderAsc();

    List<Category> findByParentCategoryIdOrderByDisplayOrderAsc(Long parentId);

    boolean existsByCategoryName(String categoryName);

    List<Category> findByIsDisplayedTrue();
}

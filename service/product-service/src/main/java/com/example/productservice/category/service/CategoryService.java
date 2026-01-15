package com.example.productservice.category.service;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    List<CategoryTreeResponse> getCategoryTree();

    CategoryResponse getCategory(Long categoryId);
}

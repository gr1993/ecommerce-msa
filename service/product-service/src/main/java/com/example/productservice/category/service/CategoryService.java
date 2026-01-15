package com.example.productservice.category.service;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);
}

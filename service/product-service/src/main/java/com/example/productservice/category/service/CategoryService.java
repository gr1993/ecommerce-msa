package com.example.productservice.category.service;

import com.example.productservice.category.dto.CatalogSyncCategoryResponse;
import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.dto.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    List<CategoryTreeResponse> getCategoryTree();

    CategoryResponse getCategory(Long categoryId);

    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request);

    void deleteCategory(Long categoryId);

    List<CatalogSyncCategoryResponse> getCategoriesForCatalogSync();
}

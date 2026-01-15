package com.example.productservice.category.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        log.info("카테고리 등록 - categoryName: {}, parentId: {}", request.getCategoryName(), request.getParentId());

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("상위 카테고리를 찾을 수 없습니다. parentId: " + request.getParentId()));
        }

        Category category = Category.builder()
                .parent(parent)
                .categoryName(request.getCategoryName())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isDisplayed(request.getIsDisplayed() != null ? request.getIsDisplayed() : true)
                .build();

        Category savedCategory = categoryRepository.save(category);
        log.info("카테고리 등록 완료 - categoryId: {}", savedCategory.getCategoryId());

        return CategoryResponse.from(savedCategory);
    }

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        log.info("카테고리 트리 조회");

        List<Category> rootCategories = categoryRepository.findByParentIsNullOrderByDisplayOrderAsc();

        List<CategoryTreeResponse> tree = rootCategories.stream()
                .map(category -> CategoryTreeResponse.from(category, 1))
                .collect(Collectors.toList());

        log.info("카테고리 트리 조회 완료 - 최상위 카테고리 수: {}", tree.size());
        return tree;
    }

    @Override
    public CategoryResponse getCategory(Long categoryId) {
        log.info("카테고리 상세 조회 - categoryId: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: " + categoryId));

        log.info("카테고리 상세 조회 완료 - categoryName: {}", category.getCategoryName());
        return CategoryResponse.from(category);
    }
}

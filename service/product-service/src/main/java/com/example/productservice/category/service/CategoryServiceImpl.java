package com.example.productservice.category.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.domain.event.CategoryCreatedEvent;
import com.example.productservice.category.domain.event.CategoryDeletedEvent;
import com.example.productservice.category.domain.event.CategoryUpdatedEvent;
import com.example.productservice.category.dto.CatalogSyncCategoryResponse;
import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.dto.CategoryUpdateRequest;
import com.example.productservice.category.repository.CategoryRepository;
import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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

        // 이벤트 생성 및 Outbox에 저장
        saveCategoryCreatedEvent(savedCategory);

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

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request) {
        log.info("카테고리 수정 - categoryId: {}, categoryName: {}", categoryId, request.getCategoryName());

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: " + categoryId));

        category.setCategoryName(request.getCategoryName());

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getIsDisplayed() != null) {
            category.setIsDisplayed(request.getIsDisplayed());
        }

        Category updatedCategory = categoryRepository.save(category);
        log.info("카테고리 수정 완료 - categoryId: {}", updatedCategory.getCategoryId());

        // 이벤트 생성 및 Outbox에 저장
        saveCategoryUpdatedEvent(updatedCategory);

        return CategoryResponse.from(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("카테고리 삭제 - categoryId: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: " + categoryId));

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new IllegalStateException("하위 카테고리가 존재하여 삭제할 수 없습니다. categoryId: " + categoryId);
        }

        // 삭제 전에 이벤트 생성 및 Outbox에 저장
        saveCategoryDeletedEvent(categoryId);

        categoryRepository.delete(category);
        log.info("카테고리 삭제 완료 - categoryId: {}", categoryId);
    }

    @Override
    public List<CatalogSyncCategoryResponse> getCategoriesForCatalogSync() {
        log.info("카탈로그 동기화용 카테고리 목록 조회");

        List<CatalogSyncCategoryResponse> result = categoryRepository.findByIsDisplayedTrue().stream()
                .map(CatalogSyncCategoryResponse::from)
                .collect(Collectors.toList());

        log.info("카탈로그 동기화용 카테고리 목록 조회 완료 - 총 카테고리 수: {}", result.size());
        return result;
    }

    private void saveCategoryCreatedEvent(Category category) {
        try {
            CategoryCreatedEvent event = new CategoryCreatedEvent(
                    category.getCategoryId(),
                    category.getParent() != null ? category.getParent().getCategoryId() : null,
                    category.getCategoryName(),
                    category.getDisplayOrder(),
                    category.getIsDisplayed(),
                    category.getCreatedAt()
            );

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Category")
                    .aggregateId(String.valueOf(category.getCategoryId()))
                    .eventType(EventTypeConstants.TOPIC_CATEGORY_CREATED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.info("CategoryCreated 이벤트가 Outbox에 저장되었습니다. categoryId: {}", category.getCategoryId());
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: categoryId={}", category.getCategoryId(), e);
            throw new RuntimeException("이벤트 저장 중 오류가 발생했습니다.", e);
        }
    }

    private void saveCategoryUpdatedEvent(Category category) {
        try {
            CategoryUpdatedEvent event = new CategoryUpdatedEvent(
                    category.getCategoryId(),
                    category.getParent() != null ? category.getParent().getCategoryId() : null,
                    category.getCategoryName(),
                    category.getDisplayOrder(),
                    category.getIsDisplayed(),
                    category.getUpdatedAt()
            );

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Category")
                    .aggregateId(String.valueOf(category.getCategoryId()))
                    .eventType(EventTypeConstants.TOPIC_CATEGORY_UPDATED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.info("CategoryUpdated 이벤트가 Outbox에 저장되었습니다. categoryId: {}", category.getCategoryId());
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: categoryId={}", category.getCategoryId(), e);
            throw new RuntimeException("이벤트 저장 중 오류가 발생했습니다.", e);
        }
    }

    private void saveCategoryDeletedEvent(Long categoryId) {
        try {
            CategoryDeletedEvent event = new CategoryDeletedEvent(
                    categoryId,
                    LocalDateTime.now()
            );

            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType("Category")
                    .aggregateId(String.valueOf(categoryId))
                    .eventType(EventTypeConstants.TOPIC_CATEGORY_DELETED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.info("CategoryDeleted 이벤트가 Outbox에 저장되었습니다. categoryId: {}", categoryId);
        } catch (JsonProcessingException e) {
            log.error("이벤트 직렬화 실패: categoryId={}", categoryId, e);
            throw new RuntimeException("이벤트 저장 중 오류가 발생했습니다.", e);
        }
    }
}

package com.example.productservice.category.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SampleCategoryDataService {

    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    @Transactional
    public List<CategoryResponse> createSampleCategories() {
        log.info("Starting sample category creation...");

        List<CategoryResponse> results = new ArrayList<>();
        List<SampleCategoryData> sampleCategories = getSampleCategoryDataList();

        for (SampleCategoryData data : sampleCategories) {
            try {
                Category category = upsertCategory(data);
                results.add(CategoryResponse.from(category));
                log.info("Upserted sample category: {} (ID: {})", data.categoryName, data.categoryId);
            } catch (Exception e) {
                log.error("Failed to upsert sample category: {} (ID: {})", data.categoryName, data.categoryId, e);
            }
        }

        log.info("Sample category creation completed. Upserted {} categories.", results.size());
        return results;
    }

    private Category upsertCategory(SampleCategoryData data) {
        Optional<Category> existingCategory = categoryRepository.findById(data.categoryId);

        if (existingCategory.isPresent()) {
            // Update existing category
            Category category = existingCategory.get();
            category.setCategoryName(data.categoryName);
            category.setDisplayOrder(data.displayOrder);
            category.setIsDisplayed(data.isDisplayed);

            if (data.parentId != null) {
                Category parent = categoryRepository.findById(data.parentId).orElse(null);
                category.setParent(parent);
            } else {
                category.setParent(null);
            }

            return categoryRepository.save(category);
        } else {
            // Insert with specific ID using native query
            entityManager.createNativeQuery(
                    "INSERT INTO category (category_id, parent_id, category_name, display_order, is_displayed, created_at, updated_at) " +
                    "VALUES (:categoryId, :parentId, :categoryName, :displayOrder, :isDisplayed, :createdAt, :updatedAt)")
                    .setParameter("categoryId", data.categoryId)
                    .setParameter("parentId", data.parentId)
                    .setParameter("categoryName", data.categoryName)
                    .setParameter("displayOrder", data.displayOrder)
                    .setParameter("isDisplayed", data.isDisplayed)
                    .setParameter("createdAt", data.createdAt)
                    .setParameter("updatedAt", data.updatedAt)
                    .executeUpdate();

            entityManager.flush();
            entityManager.clear();

            return categoryRepository.findById(data.categoryId).orElseThrow();
        }
    }

    private List<SampleCategoryData> getSampleCategoryDataList() {
        List<SampleCategoryData> categories = new ArrayList<>();

        // 1차 카테고리 (루트)
        categories.add(new SampleCategoryData(1L, null, "상의", 0, true,
                LocalDateTime.of(2026, 1, 15, 16, 23, 22),
                LocalDateTime.of(2026, 1, 20, 16, 27, 30)));

        categories.add(new SampleCategoryData(3L, null, "하의", 1, true,
                LocalDateTime.of(2026, 1, 16, 14, 5, 30),
                LocalDateTime.of(2026, 1, 16, 14, 5, 30)));

        categories.add(new SampleCategoryData(6L, null, "신발", 2, true,
                LocalDateTime.of(2026, 1, 21, 10, 30, 15),
                LocalDateTime.of(2026, 1, 21, 10, 30, 21)));

        // 2차 카테고리
        categories.add(new SampleCategoryData(4L, 1L, "남자상의", 0, true,
                LocalDateTime.of(2026, 1, 20, 16, 27, 42),
                LocalDateTime.of(2026, 1, 20, 16, 27, 42)));

        categories.add(new SampleCategoryData(9L, 3L, "남자하의", 0, true,
                LocalDateTime.of(2026, 1, 22, 17, 19, 58),
                LocalDateTime.of(2026, 1, 22, 17, 19, 58)));

        categories.add(new SampleCategoryData(7L, 6L, "스포츠", 0, true,
                LocalDateTime.of(2026, 1, 21, 10, 30, 27),
                LocalDateTime.of(2026, 1, 21, 10, 30, 27)));

        // 3차 카테고리 (leaf)
        categories.add(new SampleCategoryData(5L, 4L, "맨투맨", 0, true,
                LocalDateTime.of(2026, 1, 20, 16, 27, 49),
                LocalDateTime.of(2026, 1, 20, 16, 27, 49)));

        categories.add(new SampleCategoryData(10L, 9L, "데님바지", 0, true,
                LocalDateTime.of(2026, 1, 22, 17, 20, 20),
                LocalDateTime.of(2026, 1, 22, 17, 20, 20)));

        categories.add(new SampleCategoryData(8L, 7L, "나이키", 0, true,
                LocalDateTime.of(2026, 1, 21, 10, 30, 36),
                LocalDateTime.of(2026, 1, 21, 10, 30, 36)));

        return categories;
    }

    private static class SampleCategoryData {
        Long categoryId;
        Long parentId;
        String categoryName;
        Integer displayOrder;
        Boolean isDisplayed;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        SampleCategoryData(Long categoryId, Long parentId, String categoryName,
                           Integer displayOrder, Boolean isDisplayed,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.categoryId = categoryId;
            this.parentId = parentId;
            this.categoryName = categoryName;
            this.displayOrder = displayOrder;
            this.isDisplayed = isDisplayed;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}

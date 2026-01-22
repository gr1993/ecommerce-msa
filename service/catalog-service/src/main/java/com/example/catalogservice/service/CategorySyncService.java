package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncCategoryResponse;
import com.example.catalogservice.domain.CategoryCache;
import com.example.catalogservice.domain.CategoryTreeNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySyncService {

    private final ProductServiceClient productServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "catalog:category:";
    private static final String KEY_TEMP_PREFIX = "catalog:category:temp:";
    private static final String KEY_OLD_PREFIX = "catalog:category:old:";
    private static final String KEY_INDEX_ALL = "catalog:category:index:all";
    private static final String KEY_DISPLAY_TREE = "catalog:category:display:tree";

    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Rename 전략 기반 Full Sync.
     * 1. 새 데이터를 temp 키에 저장
     * 2. 기존 키들을 old 키로 rename
     * 3. temp 키들을 정규 키로 rename
     * 4. old 키들 삭제
     * 5. index:all에 전체 목록 JSON 저장
     * 6. display:tree에 계층 트리 구조 저장
     *
     * 이 방식은 삭제된 카테고리도 자동으로 처리된다.
     */
    public int fullSync() {
        log.info("Starting category full sync with rename strategy");

        try {
            // 1. product-service에서 카테고리 목록 조회
            List<CatalogSyncCategoryResponse> categories = productServiceClient.getCategoriesForSync();

            if (categories == null || categories.isEmpty()) {
                log.warn("No categories found from product-service");
                return 0;
            }

            log.info("Fetched {} categories from product-service", categories.size());

            // 2. 새 데이터를 temp 키에 저장
            List<CategoryCache> categoryList = categories.stream()
                    .map(this::toCategoryCache)
                    .toList();

            for (CategoryCache category : categoryList) {
                String tempKey = KEY_TEMP_PREFIX + category.getCategoryId();
                redisTemplate.opsForValue().set(tempKey, category);
            }

            log.info("Saved {} categories to temp keys", categoryList.size());

            // 3. 기존 키들을 old 키로 rename
            Set<String> existingKeys = scanKeys(KEY_PREFIX + "[0-9]*");
            for (String existingKey : existingKeys) {
                String categoryId = existingKey.substring(KEY_PREFIX.length());
                String oldKey = KEY_OLD_PREFIX + categoryId;
                redisTemplate.rename(existingKey, oldKey);
            }

            log.info("Renamed {} existing keys to old keys", existingKeys.size());

            // 4. temp 키들을 정규 키로 rename
            Set<String> tempKeys = scanKeys(KEY_TEMP_PREFIX + "*");
            for (String tempKey : tempKeys) {
                String categoryId = tempKey.substring(KEY_TEMP_PREFIX.length());
                String regularKey = KEY_PREFIX + categoryId;
                redisTemplate.rename(tempKey, regularKey);
            }

            log.info("Renamed {} temp keys to regular keys", tempKeys.size());

            // 5. old 키들 삭제
            Set<String> oldKeys = scanKeys(KEY_OLD_PREFIX + "*");
            if (!oldKeys.isEmpty()) {
                redisTemplate.delete(oldKeys);
                log.info("Deleted {} old keys", oldKeys.size());
            }

            // 6. index:all에 전체 목록 JSON 저장
            saveIndexAll(categoryList);

            // 7. display:tree에 계층 트리 구조 저장
            saveDisplayTree(categoryList);

            log.info("Category full sync completed successfully. Total categories synced: {}", categoryList.size());
            return categoryList.size();

        } catch (Exception e) {
            log.error("Category full sync failed", e);
            // 실패 시 temp 키 정리
            cleanupTempKeys();
            throw new RuntimeException("Category full sync failed", e);
        }
    }

    private CategoryCache toCategoryCache(CatalogSyncCategoryResponse response) {
        return CategoryCache.builder()
                .categoryId(response.getCategoryId())
                .parentId(response.getParentId())
                .categoryName(response.getCategoryName())
                .displayOrder(response.getDisplayOrder())
                .depth(response.getDepth())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null ? keys : Set.of();
    }

    private void saveIndexAll(List<CategoryCache> categoryList) {
        try {
            String json = objectMapper.writeValueAsString(categoryList);
            redisTemplate.opsForValue().set(KEY_INDEX_ALL, json);
            log.info("Saved index:all with {} categories", categoryList.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize category list to JSON", e);
            throw new RuntimeException("Failed to save index:all", e);
        }
    }

    private void cleanupTempKeys() {
        Set<String> tempKeys = scanKeys(KEY_TEMP_PREFIX + "*");
        if (!tempKeys.isEmpty()) {
            redisTemplate.delete(tempKeys);
            log.info("Cleaned up {} temp keys after failure", tempKeys.size());
        }
    }

    /**
     * 조회용: index:all에서 전체 카테고리 목록 조회
     */
    public List<CategoryCache> getAllCategories() {
        Object value = redisTemplate.opsForValue().get(KEY_INDEX_ALL);
        if (value == null) {
            return List.of();
        }

        try {
            String json = value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryCache.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize category list from index:all", e);
            return List.of();
        }
    }

    /**
     * 조회용: 단일 카테고리 조회
     */
    public CategoryCache getCategoryById(Long categoryId) {
        String key = KEY_PREFIX + categoryId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof CategoryCache) {
            return (CategoryCache) value;
        }
        return null;
    }

    /**
     * 플랫 목록을 계층 트리 구조로 변환
     */
    private List<CategoryTreeNode> buildTree(List<CategoryCache> flatList) {
        Map<Long, List<CategoryCache>> childrenMap = flatList.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryCache::getParentId));

        return flatList.stream()
                .filter(c -> c.getParentId() == null)
                .sorted(Comparator.comparing(CategoryCache::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(root -> buildNode(root, childrenMap))
                .toList();
    }

    private CategoryTreeNode buildNode(CategoryCache category, Map<Long, List<CategoryCache>> childrenMap) {
        List<CategoryCache> childCategories = childrenMap.get(category.getCategoryId());

        List<CategoryTreeNode> children = null;
        if (childCategories != null && !childCategories.isEmpty()) {
            children = childCategories.stream()
                    .sorted(Comparator.comparing(CategoryCache::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(child -> buildNode(child, childrenMap))
                    .toList();
        }

        return CategoryTreeNode.builder()
                .categoryId(category.getCategoryId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .displayOrder(category.getDisplayOrder())
                .depth(category.getDepth())
                .children(children)
                .build();
    }

    private void saveDisplayTree(List<CategoryCache> categoryList) {
        try {
            List<CategoryTreeNode> tree = buildTree(categoryList);
            String json = objectMapper.writeValueAsString(tree);
            redisTemplate.opsForValue().set(KEY_DISPLAY_TREE, json);
            log.info("Saved display:tree with {} root categories", tree.size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize category tree to JSON", e);
            throw new RuntimeException("Failed to save display:tree", e);
        }
    }

    /**
     * 조회용: 카테고리 트리 조회
     */
    public List<CategoryTreeNode> getCategoryTree() {
        Object value = redisTemplate.opsForValue().get(KEY_DISPLAY_TREE);
        if (value == null) {
            return List.of();
        }

        try {
            String json = value instanceof String ? (String) value : objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryTreeNode.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize category tree from display:tree", e);
            return List.of();
        }
    }
}

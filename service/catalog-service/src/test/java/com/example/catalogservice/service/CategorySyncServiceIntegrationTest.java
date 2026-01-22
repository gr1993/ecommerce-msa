package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncCategoryResponse;
import com.example.catalogservice.config.RedisTestContainerConfig;
import com.example.catalogservice.domain.CategoryCache;
import com.example.catalogservice.domain.CategoryTreeNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "springwolf.enabled=false",
        "product-service.url=http://localhost:8083"
})
@Import(RedisTestContainerConfig.class)
@DisplayName("CategorySyncService 통합 테스트 (Redis)")
class CategorySyncServiceIntegrationTest {

    @Autowired
    private CategorySyncService categorySyncService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    private static final String KEY_PREFIX = "catalog:category:";
    private static final String KEY_TEMP_PREFIX = "catalog:category:temp:";
    private static final String KEY_OLD_PREFIX = "catalog:category:old:";
    private static final String KEY_INDEX_ALL = "catalog:category:index:all";
    private static final String KEY_DISPLAY_TREE = "catalog:category:display:tree";

    @BeforeEach
    void setUp() {
        // Redis 초기화
        cleanupAllKeys();
    }

    @Test
    @DisplayName("전체 동기화 - Redis에 실제 저장 검증")
    void fullSync_RealRedisIntegration() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1),
                createMockCategory(3L, 1L, "노트북", 2, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(3);

        // Redis에 실제로 저장되었는지 확인
        CategoryCache category1 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "1");
        CategoryCache category2 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "2");
        CategoryCache category3 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "3");

        assertThat(category1).isNotNull();
        assertThat(category1.getCategoryId()).isEqualTo(1L);
        assertThat(category1.getCategoryName()).isEqualTo("전자제품");
        assertThat(category1.getParentId()).isNull();
        assertThat(category1.getDepth()).isEqualTo(0);

        assertThat(category2).isNotNull();
        assertThat(category2.getCategoryId()).isEqualTo(2L);
        assertThat(category2.getParentId()).isEqualTo(1L);

        assertThat(category3).isNotNull();
        assertThat(category3.getCategoryId()).isEqualTo(3L);
        assertThat(category3.getParentId()).isEqualTo(1L);

        // index:all 검증
        Object indexAll = redisTemplate.opsForValue().get(KEY_INDEX_ALL);
        assertThat(indexAll).isNotNull();
        assertThat(indexAll.toString()).contains("\"categoryId\":1");
        assertThat(indexAll.toString()).contains("\"categoryId\":2");
        assertThat(indexAll.toString()).contains("\"categoryId\":3");

        // temp, old 키가 정리되었는지 확인
        Set<String> tempKeys = redisTemplate.keys(KEY_TEMP_PREFIX + "*");
        Set<String> oldKeys = redisTemplate.keys(KEY_OLD_PREFIX + "*");
        assertThat(tempKeys).isEmpty();
        assertThat(oldKeys).isEmpty();
    }

    @Test
    @DisplayName("전체 동기화 - 기존 데이터 갱신 (rename 전략)")
    void fullSync_UpdateExistingData() {
        // Given - 초기 데이터 저장
        CategoryCache oldCategory1 = CategoryCache.builder()
                .categoryId(1L)
                .categoryName("구형 전자제품")
                .displayOrder(1)
                .depth(0)
                .build();
        CategoryCache oldCategory2 = CategoryCache.builder()
                .categoryId(2L)
                .categoryName("구형 스마트폰")
                .displayOrder(1)
                .depth(1)
                .build();

        redisTemplate.opsForValue().set(KEY_PREFIX + "1", oldCategory1);
        redisTemplate.opsForValue().set(KEY_PREFIX + "2", oldCategory2);

        // 새 데이터 (카테고리 2 삭제, 카테고리 1 갱신, 카테고리 3 추가)
        List<CatalogSyncCategoryResponse> newCategories = List.of(
                createMockCategory(1L, null, "최신 전자제품", 1, 0),
                createMockCategory(3L, 1L, "태블릿", 3, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(newCategories);

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(2);

        // 카테고리 1은 갱신됨
        CategoryCache category1 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "1");
        assertThat(category1).isNotNull();
        assertThat(category1.getCategoryName()).isEqualTo("최신 전자제품");

        // 카테고리 2는 삭제됨 (rename 전략으로 자동 처리)
        CategoryCache category2 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "2");
        assertThat(category2).isNull();

        // 카테고리 3은 추가됨
        CategoryCache category3 = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "3");
        assertThat(category3).isNotNull();
        assertThat(category3.getCategoryName()).isEqualTo("태블릿");
    }

    @Test
    @DisplayName("전체 동기화 - 대량 카테고리 처리")
    void fullSync_LargeDataset() {
        // Given
        List<CatalogSyncCategoryResponse> categories = createMultipleCategories(0, 100);
        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(100);

        // 일부 샘플 검증
        CategoryCache first = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "0");
        CategoryCache middle = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "50");
        CategoryCache last = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "99");

        assertThat(first).isNotNull();
        assertThat(middle).isNotNull();
        assertThat(last).isNotNull();

        // 전체 키 개수 확인
        Set<String> allKeys = redisTemplate.keys(KEY_PREFIX + "[0-9]*");
        assertThat(allKeys).hasSize(100);
    }

    @Test
    @DisplayName("getAllCategories - Redis에서 실제 조회")
    void getAllCategories_RealRedis() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        categorySyncService.fullSync();

        // When
        List<CategoryCache> result = categorySyncService.getAllCategories();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryCache::getCategoryId)
                .containsExactly(1L, 2L);
        assertThat(result).extracting(CategoryCache::getCategoryName)
                .containsExactly("전자제품", "스마트폰");
    }

    @Test
    @DisplayName("getAllCategories - 데이터가 없을 때")
    void getAllCategories_NoData() {
        // When
        List<CategoryCache> result = categorySyncService.getAllCategories();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryById - Redis에서 실제 조회")
    void getCategoryById_RealRedis() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(100L, null, "테스트 카테고리", 1, 0)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        categorySyncService.fullSync();

        // When
        CategoryCache result = categorySyncService.getCategoryById(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(100L);
        assertThat(result.getCategoryName()).isEqualTo("테스트 카테고리");
    }

    @Test
    @DisplayName("getCategoryById - 존재하지 않는 카테고리")
    void getCategoryById_NotFound() {
        // When
        CategoryCache result = categorySyncService.getCategoryById(999L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("rename 전략 - temp 키가 실패 시 정리됨")
    void fullSync_TempKeysCleanedOnFailure() {
        // Given
        when(productServiceClient.getCategoriesForSync())
                .thenThrow(new RuntimeException("External service failure"));

        // When
        try {
            categorySyncService.fullSync();
        } catch (Exception e) {
            // 예외 무시
        }

        // Then
        Set<String> tempKeys = redisTemplate.keys(KEY_TEMP_PREFIX + "*");
        assertThat(tempKeys).isEmpty();
    }

    @Test
    @DisplayName("CategoryCache Serialization/Deserialization 검증")
    void categoryCache_Serialization() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategoryWithTimestamp(1L, null, "전자제품", 1, 0, now, now)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);

        // When
        categorySyncService.fullSync();

        // Then
        CategoryCache result = (CategoryCache) redisTemplate.opsForValue().get(KEY_PREFIX + "1");

        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getParentId()).isNull();
        assertThat(result.getCategoryName()).isEqualTo("전자제품");
        assertThat(result.getDisplayOrder()).isEqualTo(1);
        assertThat(result.getDepth()).isEqualTo(0);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    private void cleanupAllKeys() {
        Set<String> allKeys = redisTemplate.keys("catalog:category:*");
        if (allKeys != null && !allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
        }
    }

    private CatalogSyncCategoryResponse createMockCategory(Long id, Long parentId, String name,
                                                          Integer displayOrder, Integer depth) {
        return createMockCategoryWithTimestamp(id, parentId, name, displayOrder, depth,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private CatalogSyncCategoryResponse createMockCategoryWithTimestamp(Long id, Long parentId, String name,
                                                                       Integer displayOrder, Integer depth,
                                                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        try {
            java.lang.reflect.Constructor<CatalogSyncCategoryResponse> constructor =
                    CatalogSyncCategoryResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CatalogSyncCategoryResponse category = constructor.newInstance();

            ReflectionTestUtils.setField(category, "categoryId", id);
            ReflectionTestUtils.setField(category, "parentId", parentId);
            ReflectionTestUtils.setField(category, "categoryName", name);
            ReflectionTestUtils.setField(category, "displayOrder", displayOrder);
            ReflectionTestUtils.setField(category, "depth", depth);
            ReflectionTestUtils.setField(category, "createdAt", createdAt);
            ReflectionTestUtils.setField(category, "updatedAt", updatedAt);
            return category;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock category", e);
        }
    }

    private List<CatalogSyncCategoryResponse> createMultipleCategories(int startId, int count) {
        return java.util.stream.IntStream.range(startId, startId + count)
                .mapToObj(i -> createMockCategory((long) i, null, "Category " + i, i, 0))
                .toList();
    }

    @Test
    @DisplayName("fullSync - display:tree에 트리 구조 저장 검증 (통합)")
    void fullSync_DisplayTreeSavedToRedis() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1),
                createMockCategory(3L, 1L, "노트북", 2, 1),
                createMockCategory(4L, 2L, "삼성", 1, 2),
                createMockCategory(5L, 2L, "애플", 2, 2)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(5);

        // Redis에 display:tree가 저장되었는지 확인
        Object treeValue = redisTemplate.opsForValue().get(KEY_DISPLAY_TREE);
        assertThat(treeValue).isNotNull();
        assertThat(treeValue.toString()).contains("\"categoryId\":1");
        assertThat(treeValue.toString()).contains("\"categoryName\":\"전자제품\"");
        assertThat(treeValue.toString()).contains("\"children\"");
    }

    @Test
    @DisplayName("getCategoryTree - 계층 구조 조회 (통합)")
    void getCategoryTree_RealRedisIntegration() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, null, "의류", 2, 0),
                createMockCategory(3L, 1L, "스마트폰", 1, 1),
                createMockCategory(4L, 1L, "노트북", 2, 1),
                createMockCategory(5L, 3L, "삼성", 1, 2),
                createMockCategory(6L, 3L, "애플", 2, 2),
                createMockCategory(7L, 2L, "남성의류", 1, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        categorySyncService.fullSync();

        // When
        List<CategoryTreeNode> tree = categorySyncService.getCategoryTree();

        // Then
        assertThat(tree).hasSize(2); // 두 개의 루트 카테고리

        // 첫 번째 루트: 전자제품
        CategoryTreeNode electronics = tree.get(0);
        assertThat(electronics.getCategoryId()).isEqualTo(1L);
        assertThat(electronics.getCategoryName()).isEqualTo("전자제품");
        assertThat(electronics.getParentId()).isNull();
        assertThat(electronics.getDepth()).isEqualTo(0);
        assertThat(electronics.getChildren()).hasSize(2);

        // 전자제품의 자식들
        CategoryTreeNode smartphone = electronics.getChildren().get(0);
        assertThat(smartphone.getCategoryId()).isEqualTo(3L);
        assertThat(smartphone.getCategoryName()).isEqualTo("스마트폰");
        assertThat(smartphone.getParentId()).isEqualTo(1L);
        assertThat(smartphone.getChildren()).hasSize(2);

        CategoryTreeNode laptop = electronics.getChildren().get(1);
        assertThat(laptop.getCategoryId()).isEqualTo(4L);
        assertThat(laptop.getCategoryName()).isEqualTo("노트북");
        assertThat(laptop.getParentId()).isEqualTo(1L);
        assertThat(laptop.getChildren()).isNull(); // 자식이 없음

        // 스마트폰의 자식들
        CategoryTreeNode samsung = smartphone.getChildren().get(0);
        assertThat(samsung.getCategoryId()).isEqualTo(5L);
        assertThat(samsung.getCategoryName()).isEqualTo("삼성");
        assertThat(samsung.getDepth()).isEqualTo(2);
        assertThat(samsung.getChildren()).isNull();

        CategoryTreeNode apple = smartphone.getChildren().get(1);
        assertThat(apple.getCategoryId()).isEqualTo(6L);
        assertThat(apple.getCategoryName()).isEqualTo("애플");
        assertThat(apple.getDepth()).isEqualTo(2);

        // 두 번째 루트: 의류
        CategoryTreeNode clothing = tree.get(1);
        assertThat(clothing.getCategoryId()).isEqualTo(2L);
        assertThat(clothing.getCategoryName()).isEqualTo("의류");
        assertThat(clothing.getChildren()).hasSize(1);

        CategoryTreeNode menClothing = clothing.getChildren().get(0);
        assertThat(menClothing.getCategoryId()).isEqualTo(7L);
        assertThat(menClothing.getCategoryName()).isEqualTo("남성의류");
    }

    @Test
    @DisplayName("getCategoryTree - displayOrder 정렬 검증 (통합)")
    void getCategoryTree_SortedByDisplayOrder() {
        // Given - displayOrder가 역순으로 들어옴
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "C카테고리", 3, 0),
                createMockCategory(2L, null, "A카테고리", 1, 0),
                createMockCategory(3L, null, "B카테고리", 2, 0),
                createMockCategory(4L, 2L, "A-2", 2, 1),
                createMockCategory(5L, 2L, "A-1", 1, 1),
                createMockCategory(6L, 2L, "A-3", 3, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        categorySyncService.fullSync();

        // When
        List<CategoryTreeNode> tree = categorySyncService.getCategoryTree();

        // Then
        assertThat(tree).hasSize(3);

        // 루트 레벨 정렬 확인 (displayOrder 오름차순)
        assertThat(tree.get(0).getCategoryName()).isEqualTo("A카테고리");
        assertThat(tree.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(tree.get(1).getCategoryName()).isEqualTo("B카테고리");
        assertThat(tree.get(1).getDisplayOrder()).isEqualTo(2);
        assertThat(tree.get(2).getCategoryName()).isEqualTo("C카테고리");
        assertThat(tree.get(2).getDisplayOrder()).isEqualTo(3);

        // 자식 레벨 정렬 확인
        CategoryTreeNode aCategory = tree.get(0);
        assertThat(aCategory.getChildren()).hasSize(3);
        assertThat(aCategory.getChildren().get(0).getCategoryName()).isEqualTo("A-1");
        assertThat(aCategory.getChildren().get(1).getCategoryName()).isEqualTo("A-2");
        assertThat(aCategory.getChildren().get(2).getCategoryName()).isEqualTo("A-3");
    }

    @Test
    @DisplayName("getCategoryTree - 빈 트리 조회")
    void getCategoryTree_EmptyTree() {
        // When
        List<CategoryTreeNode> tree = categorySyncService.getCategoryTree();

        // Then
        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("getCategoryTree - 단일 루트, 다중 depth 트리")
    void getCategoryTree_DeepHierarchy() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "루트", 1, 0),
                createMockCategory(2L, 1L, "Depth1", 1, 1),
                createMockCategory(3L, 2L, "Depth2", 1, 2),
                createMockCategory(4L, 3L, "Depth3", 1, 3),
                createMockCategory(5L, 4L, "Depth4", 1, 4)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        categorySyncService.fullSync();

        // When
        List<CategoryTreeNode> tree = categorySyncService.getCategoryTree();

        // Then
        assertThat(tree).hasSize(1);

        CategoryTreeNode root = tree.get(0);
        assertThat(root.getDepth()).isEqualTo(0);

        CategoryTreeNode depth1 = root.getChildren().get(0);
        assertThat(depth1.getDepth()).isEqualTo(1);

        CategoryTreeNode depth2 = depth1.getChildren().get(0);
        assertThat(depth2.getDepth()).isEqualTo(2);

        CategoryTreeNode depth3 = depth2.getChildren().get(0);
        assertThat(depth3.getDepth()).isEqualTo(3);

        CategoryTreeNode depth4 = depth3.getChildren().get(0);
        assertThat(depth4.getDepth()).isEqualTo(4);
        assertThat(depth4.getChildren()).isNull(); // leaf 노드
    }

    @Test
    @DisplayName("fullSync 후 트리 업데이트 검증")
    void fullSync_TreeUpdatedOnSync() {
        // Given - 초기 트리
        List<CatalogSyncCategoryResponse> initialCategories = List.of(
                createMockCategory(1L, null, "구형 전자제품", 1, 0),
                createMockCategory(2L, 1L, "구형 스마트폰", 1, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(initialCategories);
        categorySyncService.fullSync();

        List<CategoryTreeNode> initialTree = categorySyncService.getCategoryTree();
        assertThat(initialTree).hasSize(1);
        assertThat(initialTree.get(0).getCategoryName()).isEqualTo("구형 전자제품");

        // When - 새로운 트리로 업데이트
        List<CatalogSyncCategoryResponse> newCategories = List.of(
                createMockCategory(1L, null, "최신 전자제품", 1, 0),
                createMockCategory(3L, 1L, "태블릿", 2, 1) // 카테고리 2 삭제, 3 추가
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(newCategories);
        categorySyncService.fullSync();

        // Then
        List<CategoryTreeNode> updatedTree = categorySyncService.getCategoryTree();
        assertThat(updatedTree).hasSize(1);
        assertThat(updatedTree.get(0).getCategoryName()).isEqualTo("최신 전자제품");
        assertThat(updatedTree.get(0).getChildren()).hasSize(1);
        assertThat(updatedTree.get(0).getChildren().get(0).getCategoryName()).isEqualTo("태블릿");
    }
}

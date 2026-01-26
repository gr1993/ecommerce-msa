package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncCategoryResponse;
import com.example.catalogservice.consumer.event.CategoryCreatedEvent;
import com.example.catalogservice.consumer.event.CategoryDeletedEvent;
import com.example.catalogservice.consumer.event.CategoryUpdatedEvent;
import com.example.catalogservice.domain.CategoryCache;
import com.example.catalogservice.domain.CategoryTreeNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategorySyncService 단위 테스트")
class CategorySyncServiceTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CategorySyncService categorySyncService;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<Object> valueCaptor;

    private static final String KEY_PREFIX = "catalog:category:";
    private static final String KEY_TEMP_PREFIX = "catalog:category:temp:";
    private static final String KEY_OLD_PREFIX = "catalog:category:old:";
    private static final String KEY_INDEX_ALL = "catalog:category:index:all";
    private static final String KEY_DISPLAY_TREE = "catalog:category:display:tree";

    @BeforeEach
    void setUp() {
        categorySyncService = new CategorySyncService(productServiceClient, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("전체 동기화 - 성공")
    void fullSync_Success() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1),
                createMockCategory(3L, 1L, "노트북", 2, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1",
                KEY_TEMP_PREFIX + "2",
                KEY_TEMP_PREFIX + "3"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(3);
        verify(productServiceClient).getCategoriesForSync();
        verify(valueOperations, times(3)).set(startsWith(KEY_TEMP_PREFIX), any(CategoryCache.class));
        verify(valueOperations, times(1)).set(eq(KEY_INDEX_ALL), anyString());
        verify(redisTemplate, times(3)).rename(startsWith(KEY_TEMP_PREFIX), startsWith(KEY_PREFIX));
    }

    @Test
    @DisplayName("전체 동기화 - 빈 응답 처리")
    void fullSync_EmptyResponse() {
        // Given
        when(productServiceClient.getCategoriesForSync()).thenReturn(null);

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(productServiceClient).getCategoriesForSync();
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    @DisplayName("전체 동기화 - 빈 리스트 처리")
    void fullSync_EmptyList() {
        // Given
        when(productServiceClient.getCategoriesForSync()).thenReturn(List.of());

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(productServiceClient).getCategoriesForSync();
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    @DisplayName("전체 동기화 - 기존 키 rename 동작 검증")
    void fullSync_ExistingKeysRenamed() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0)
        );

        Set<String> existingKeys = Set.of(
                KEY_PREFIX + "1",
                KEY_PREFIX + "2",
                KEY_PREFIX + "999"  // 삭제될 카테고리
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(existingKeys);
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(KEY_TEMP_PREFIX + "1"));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of(
                KEY_OLD_PREFIX + "1",
                KEY_OLD_PREFIX + "2",
                KEY_OLD_PREFIX + "999"
        ));

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(1);
        // 기존 키들이 old 키로 rename되었는지 확인
        verify(redisTemplate, times(3)).rename(startsWith(KEY_PREFIX), startsWith(KEY_OLD_PREFIX));
        // old 키들이 삭제되었는지 확인
        verify(redisTemplate).delete(argThat((Set<String> keys) ->
            keys != null && keys.size() == 3
        ));
    }

    @Test
    @DisplayName("전체 동기화 - temp 키에서 정규 키로 rename 검증")
    void fullSync_TempKeysRenamedToRegular() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(10L, null, "신규 카테고리", 1, 0)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(KEY_TEMP_PREFIX + "10"));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        int result = categorySyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(1);
        verify(redisTemplate).rename(KEY_TEMP_PREFIX + "10", KEY_PREFIX + "10");
    }

    @Test
    @DisplayName("전체 동기화 - index:all에 JSON 저장 검증")
    void fullSync_IndexAllSaved() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1",
                KEY_TEMP_PREFIX + "2"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        verify(valueOperations).set(eq(KEY_INDEX_ALL), argThat(value -> {
            if (value instanceof String) {
                String json = (String) value;
                return json.contains("\"categoryId\":1") && json.contains("\"categoryId\":2");
            }
            return false;
        }));
    }

    @Test
    @DisplayName("전체 동기화 실패 시 temp 키 정리")
    void fullSync_Failure_CleanupTempKeys() {
        // Given
        when(productServiceClient.getCategoriesForSync())
                .thenThrow(new RuntimeException("External service failure"));

        Set<String> tempKeys = Set.of(KEY_TEMP_PREFIX + "1", KEY_TEMP_PREFIX + "2");
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(tempKeys);

        // When & Then
        assertThatThrownBy(() -> categorySyncService.fullSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Category full sync failed");

        verify(redisTemplate).delete(tempKeys);
    }

    @Test
    @DisplayName("전체 동기화 실패 시 temp 키가 없으면 정리 생략")
    void fullSync_Failure_NoTempKeysToCleanup() {
        // Given
        when(productServiceClient.getCategoriesForSync())
                .thenThrow(new RuntimeException("External service failure"));
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of());

        // When & Then
        assertThatThrownBy(() -> categorySyncService.fullSync())
                .isInstanceOf(RuntimeException.class);

        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("getAllCategories - 정상 조회")
    void getAllCategories_Success() throws JsonProcessingException {
        // Given
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0}," +
                "{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1}]";
        when(valueOperations.get(KEY_INDEX_ALL)).thenReturn(json);

        // When
        List<CategoryCache> result = categorySyncService.getAllCategories();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        assertThat(result.get(0).getCategoryName()).isEqualTo("전자제품");
        assertThat(result.get(1).getCategoryId()).isEqualTo(2L);
        assertThat(result.get(1).getCategoryName()).isEqualTo("스마트폰");
    }

    @Test
    @DisplayName("getAllCategories - 데이터 없음")
    void getAllCategories_NoData() {
        // Given
        when(valueOperations.get(KEY_INDEX_ALL)).thenReturn(null);

        // When
        List<CategoryCache> result = categorySyncService.getAllCategories();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllCategories - JSON 파싱 실패 시 빈 리스트 반환")
    void getAllCategories_JsonParsingFailed() {
        // Given
        when(valueOperations.get(KEY_INDEX_ALL)).thenReturn("invalid json");

        // When
        List<CategoryCache> result = categorySyncService.getAllCategories();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryById - 정상 조회")
    void getCategoryById_Success() {
        // Given
        CategoryCache category = CategoryCache.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .displayOrder(1)
                .depth(0)
                .build();

        when(valueOperations.get(KEY_PREFIX + "1")).thenReturn(category);

        // When
        CategoryCache result = categorySyncService.getCategoryById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getCategoryName()).isEqualTo("전자제품");
    }

    @Test
    @DisplayName("getCategoryById - 데이터 없음")
    void getCategoryById_NoData() {
        // Given
        when(valueOperations.get(KEY_PREFIX + "999")).thenReturn(null);

        // When
        CategoryCache result = categorySyncService.getCategoryById(999L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getCategoryById - CategoryCache 타입이 아니면 null 반환")
    void getCategoryById_WrongType() {
        // Given
        when(valueOperations.get(KEY_PREFIX + "1")).thenReturn("wrong type");

        // When
        CategoryCache result = categorySyncService.getCategoryById(1L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CategoryCache 변환 검증")
    void toCategoryCache_Mapping() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        CatalogSyncCategoryResponse response = createMockCategory(100L, 10L, "테스트 카테고리", 5, 2);
        ReflectionTestUtils.setField(response, "createdAt", now);
        ReflectionTestUtils.setField(response, "updatedAt", now);

        List<CatalogSyncCategoryResponse> categories = List.of(response);

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(KEY_TEMP_PREFIX + "100"));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        verify(valueOperations).set(eq(KEY_TEMP_PREFIX + "100"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getCategoryId().equals(100L) &&
                        cache.getParentId().equals(10L) &&
                        cache.getCategoryName().equals("테스트 카테고리") &&
                        cache.getDisplayOrder().equals(5) &&
                        cache.getDepth().equals(2) &&
                        cache.getCreatedAt().equals(now) &&
                        cache.getUpdatedAt().equals(now);
            }
            return false;
        }));
    }

    @Test
    @DisplayName("fullSync - display:tree에 트리 구조 저장 검증")
    void fullSync_DisplayTreeSaved() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, 1L, "스마트폰", 1, 1),
                createMockCategory(3L, 1L, "노트북", 2, 1),
                createMockCategory(4L, 2L, "삼성", 1, 2),
                createMockCategory(5L, 2L, "애플", 2, 2)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1",
                KEY_TEMP_PREFIX + "2",
                KEY_TEMP_PREFIX + "3",
                KEY_TEMP_PREFIX + "4",
                KEY_TEMP_PREFIX + "5"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), argThat(value -> {
            if (value instanceof String) {
                String json = (String) value;
                // 루트 카테고리가 포함되어 있는지 확인
                return json.contains("\"categoryId\":1") &&
                       json.contains("\"categoryName\":\"전자제품\"") &&
                       json.contains("\"children\"") &&
                       json.contains("\"categoryId\":2") &&
                       json.contains("\"categoryId\":3");
            }
            return false;
        }));
    }

    @Test
    @DisplayName("getCategoryTree - 정상 조회")
    void getCategoryTree_Success() throws JsonProcessingException {
        // Given
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1,\"children\":null}," +
                "{\"categoryId\":3,\"parentId\":1,\"categoryName\":\"노트북\",\"displayOrder\":2,\"depth\":1,\"children\":null}]}]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When
        List<CategoryTreeNode> result = categorySyncService.getCategoryTree();

        // Then
        assertThat(result).hasSize(1);

        CategoryTreeNode root = result.get(0);
        assertThat(root.getCategoryId()).isEqualTo(1L);
        assertThat(root.getCategoryName()).isEqualTo("전자제품");
        assertThat(root.getParentId()).isNull();
        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.getChildren()).hasSize(2);

        CategoryTreeNode child1 = root.getChildren().get(0);
        assertThat(child1.getCategoryId()).isEqualTo(2L);
        assertThat(child1.getCategoryName()).isEqualTo("스마트폰");
        assertThat(child1.getParentId()).isEqualTo(1L);

        CategoryTreeNode child2 = root.getChildren().get(1);
        assertThat(child2.getCategoryId()).isEqualTo(3L);
        assertThat(child2.getCategoryName()).isEqualTo("노트북");
        assertThat(child2.getParentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCategoryTree - 데이터 없음")
    void getCategoryTree_NoData() {
        // Given
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(null);

        // When
        List<CategoryTreeNode> result = categorySyncService.getCategoryTree();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryTree - JSON 파싱 실패 시 빈 리스트 반환")
    void getCategoryTree_JsonParsingFailed() {
        // Given
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn("invalid json");

        // When
        List<CategoryTreeNode> result = categorySyncService.getCategoryTree();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("buildTree - 다중 레벨 계층 구조 변환")
    void buildTree_MultiLevelHierarchy() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "전자제품", 1, 0),
                createMockCategory(2L, null, "의류", 2, 0),
                createMockCategory(3L, 1L, "스마트폰", 1, 1),
                createMockCategory(4L, 1L, "노트북", 2, 1),
                createMockCategory(5L, 3L, "삼성", 1, 2),
                createMockCategory(6L, 3L, "애플", 2, 2),
                createMockCategory(7L, 2L, "남성의류", 1, 1),
                createMockCategory(8L, 2L, "여성의류", 2, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1", KEY_TEMP_PREFIX + "2", KEY_TEMP_PREFIX + "3",
                KEY_TEMP_PREFIX + "4", KEY_TEMP_PREFIX + "5", KEY_TEMP_PREFIX + "6",
                KEY_TEMP_PREFIX + "7", KEY_TEMP_PREFIX + "8"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        // display:tree에 저장되는지 확인
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), argThat(value -> {
            if (value instanceof String) {
                String json = (String) value;
                // 두 개의 루트 카테고리가 있어야 함
                return json.contains("\"categoryName\":\"전자제품\"") &&
                       json.contains("\"categoryName\":\"의류\"") &&
                       // 자식 카테고리들도 포함
                       json.contains("\"categoryName\":\"스마트폰\"") &&
                       json.contains("\"categoryName\":\"삼성\"") &&
                       json.contains("\"categoryName\":\"남성의류\"");
            }
            return false;
        }));
    }

    @Test
    @DisplayName("buildTree - displayOrder 기준 정렬 검증")
    void buildTree_SortedByDisplayOrder() {
        // Given - displayOrder가 역순으로 들어옴
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "C카테고리", 3, 0),
                createMockCategory(2L, null, "A카테고리", 1, 0),
                createMockCategory(3L, null, "B카테고리", 2, 0),
                createMockCategory(4L, 2L, "A-2", 2, 1),
                createMockCategory(5L, 2L, "A-1", 1, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1", KEY_TEMP_PREFIX + "2", KEY_TEMP_PREFIX + "3",
                KEY_TEMP_PREFIX + "4", KEY_TEMP_PREFIX + "5"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    @Test
    @DisplayName("buildTree - 자식이 없는 leaf 노드 처리")
    void buildTree_LeafNodesWithoutChildren() {
        // Given
        List<CatalogSyncCategoryResponse> categories = List.of(
                createMockCategory(1L, null, "루트", 1, 0),
                createMockCategory(2L, 1L, "리프1", 1, 1),
                createMockCategory(3L, 1L, "리프2", 2, 1)
        );

        when(productServiceClient.getCategoriesForSync()).thenReturn(categories);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());
        when(redisTemplate.keys(KEY_TEMP_PREFIX + "*")).thenReturn(Set.of(
                KEY_TEMP_PREFIX + "1", KEY_TEMP_PREFIX + "2", KEY_TEMP_PREFIX + "3"
        ));
        when(redisTemplate.keys(KEY_OLD_PREFIX + "*")).thenReturn(Set.of());

        // When
        categorySyncService.fullSync();

        // Then
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), argThat(value -> {
            if (value instanceof String) {
                String json = (String) value;
                // children:null이 포함되어야 함 (leaf 노드)
                return json.contains("\"categoryName\":\"리프1\"") &&
                       json.contains("\"categoryName\":\"리프2\"");
            }
            return false;
        }));
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - null 입력 시 빈 리스트 반환")
    void getCategoryIdWithDescendants_NullInput() {
        // When
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 트리가 비어있으면 자기 자신만 반환")
    void getCategoryIdWithDescendants_EmptyTree() {
        // Given
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(null);

        // When
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(100L);

        // Then
        assertThat(result).containsExactly(100L);
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 트리에서 찾지 못한 카테고리는 자기 자신만 반환")
    void getCategoryIdWithDescendants_NotFoundInTree() throws JsonProcessingException {
        // Given
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1,\"children\":null}]}]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When - 트리에 없는 카테고리 ID 999
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(999L);

        // Then
        assertThat(result).containsExactly(999L);
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 루트 카테고리 조회 시 모든 하위 카테고리 ID 포함")
    void getCategoryIdWithDescendants_RootCategory() throws JsonProcessingException {
        // Given - 전자제품(1) > 스마트폰(2) > 삼성(4), 애플(5)
        //                      > 노트북(3)
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[" +
                "{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1," +
                "\"children\":[" +
                "{\"categoryId\":4,\"parentId\":2,\"categoryName\":\"삼성\",\"displayOrder\":1,\"depth\":2,\"children\":null}," +
                "{\"categoryId\":5,\"parentId\":2,\"categoryName\":\"애플\",\"displayOrder\":2,\"depth\":2,\"children\":null}" +
                "]}," +
                "{\"categoryId\":3,\"parentId\":1,\"categoryName\":\"노트북\",\"displayOrder\":2,\"depth\":1,\"children\":null}" +
                "]}]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When - 루트 카테고리(1) 조회
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(1L);

        // Then - 자신과 모든 하위 카테고리 포함
        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 중간 카테고리 조회 시 하위 카테고리만 포함")
    void getCategoryIdWithDescendants_MiddleCategory() throws JsonProcessingException {
        // Given - 전자제품(1) > 스마트폰(2) > 삼성(4), 애플(5)
        //                      > 노트북(3)
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[" +
                "{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1," +
                "\"children\":[" +
                "{\"categoryId\":4,\"parentId\":2,\"categoryName\":\"삼성\",\"displayOrder\":1,\"depth\":2,\"children\":null}," +
                "{\"categoryId\":5,\"parentId\":2,\"categoryName\":\"애플\",\"displayOrder\":2,\"depth\":2,\"children\":null}" +
                "]}," +
                "{\"categoryId\":3,\"parentId\":1,\"categoryName\":\"노트북\",\"displayOrder\":2,\"depth\":1,\"children\":null}" +
                "]}]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When - 스마트폰 카테고리(2) 조회
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(2L);

        // Then - 자신과 하위 카테고리만 포함 (삼성, 애플)
        assertThat(result).containsExactlyInAnyOrder(2L, 4L, 5L);
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 리프 카테고리 조회 시 자기 자신만 반환")
    void getCategoryIdWithDescendants_LeafCategory() throws JsonProcessingException {
        // Given - 전자제품(1) > 스마트폰(2) > 삼성(4)
        String json = "[{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[" +
                "{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1," +
                "\"children\":[" +
                "{\"categoryId\":4,\"parentId\":2,\"categoryName\":\"삼성\",\"displayOrder\":1,\"depth\":2,\"children\":null}" +
                "]}" +
                "]}]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When - 리프 카테고리(4) 조회
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(4L);

        // Then - 자기 자신만 포함
        assertThat(result).containsExactly(4L);
    }

    @Test
    @DisplayName("getCategoryIdWithDescendants - 다중 루트 트리에서 특정 서브트리만 조회")
    void getCategoryIdWithDescendants_MultipleRootTrees() throws JsonProcessingException {
        // Given - 전자제품(1) > 스마트폰(2)
        //         의류(10) > 남성의류(11), 여성의류(12)
        String json = "[" +
                "{\"categoryId\":1,\"parentId\":null,\"categoryName\":\"전자제품\",\"displayOrder\":1,\"depth\":0," +
                "\"children\":[{\"categoryId\":2,\"parentId\":1,\"categoryName\":\"스마트폰\",\"displayOrder\":1,\"depth\":1,\"children\":null}]}," +
                "{\"categoryId\":10,\"parentId\":null,\"categoryName\":\"의류\",\"displayOrder\":2,\"depth\":0," +
                "\"children\":[" +
                "{\"categoryId\":11,\"parentId\":10,\"categoryName\":\"남성의류\",\"displayOrder\":1,\"depth\":1,\"children\":null}," +
                "{\"categoryId\":12,\"parentId\":10,\"categoryName\":\"여성의류\",\"displayOrder\":2,\"depth\":1,\"children\":null}" +
                "]}" +
                "]";
        when(valueOperations.get(KEY_DISPLAY_TREE)).thenReturn(json);

        // When - 의류 카테고리(10) 조회
        List<Long> result = categorySyncService.getCategoryIdWithDescendants(10L);

        // Then - 의류와 하위 카테고리만 포함 (전자제품 트리는 제외)
        assertThat(result).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    @DisplayName("syncCategory - 카테고리 생성 이벤트 처리 성공")
    void syncCategory_Success() {
        // Given
        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(1L)
                .parentId(null)
                .categoryName("전자제품")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build();

        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());

        // When
        categorySyncService.syncCategory(event);

        // Then
        verify(valueOperations).set(eq(KEY_PREFIX + "1"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getCategoryId().equals(1L) &&
                        cache.getCategoryName().equals("전자제품") &&
                        cache.getParentId() == null &&
                        cache.getDisplayOrder().equals(1) &&
                        cache.getDepth().equals(0);
            }
            return false;
        }));
        verify(valueOperations).set(eq(KEY_INDEX_ALL), anyString());
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    @Test
    @DisplayName("syncCategory - 하위 카테고리 생성 시 depth 자동 계산")
    void syncCategory_SubCategoryDepthCalculated() {
        // Given - 부모 카테고리가 이미 존재
        CategoryCache parentCategory = CategoryCache.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .displayOrder(1)
                .depth(0)
                .build();

        when(valueOperations.get(KEY_PREFIX + "1")).thenReturn(parentCategory);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of(KEY_PREFIX + "1"));

        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(10L)
                .parentId(1L)
                .categoryName("스마트폰")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.syncCategory(event);

        // Then
        verify(valueOperations).set(eq(KEY_PREFIX + "10"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getCategoryId().equals(10L) &&
                        cache.getCategoryName().equals("스마트폰") &&
                        cache.getParentId().equals(1L) &&
                        cache.getDepth().equals(1);  // 부모 depth(0) + 1
            }
            return false;
        }));
    }

    @Test
    @DisplayName("syncCategory - 부모 카테고리가 없는 경우 depth=1로 가정")
    void syncCategory_ParentNotFoundDepthAssumed() {
        // Given - 부모 ID는 있지만 실제 부모 카테고리가 Redis에 없음
        when(valueOperations.get(KEY_PREFIX + "999")).thenReturn(null);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());

        CategoryCreatedEvent event = CategoryCreatedEvent.builder()
                .categoryId(10L)
                .parentId(999L)
                .categoryName("고아 카테고리")
                .displayOrder(1)
                .createdAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.syncCategory(event);

        // Then
        verify(valueOperations).set(eq(KEY_PREFIX + "10"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getDepth().equals(1);  // 부모를 찾지 못하면 depth=1로 가정
            }
            return false;
        }));
    }

    @Test
    @DisplayName("updateCategory - 카테고리 수정 이벤트 처리 성공")
    void updateCategory_Success() {
        // Given - 기존 카테고리 존재
        CategoryCache existingCategory = CategoryCache.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .parentId(null)
                .displayOrder(1)
                .depth(0)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(valueOperations.get(KEY_PREFIX + "1")).thenReturn(existingCategory);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of(KEY_PREFIX + "1"));

        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(1L)
                .parentId(null)
                .categoryName("전자제품 (수정됨)")
                .displayOrder(2)
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.updateCategory(event);

        // Then
        verify(valueOperations).set(eq(KEY_PREFIX + "1"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getCategoryId().equals(1L) &&
                        cache.getCategoryName().equals("전자제품 (수정됨)") &&
                        cache.getDisplayOrder().equals(2) &&
                        cache.getCreatedAt() != null &&  // createdAt 보존
                        cache.getUpdatedAt() != null;
            }
            return false;
        }));
        verify(valueOperations).set(eq(KEY_INDEX_ALL), anyString());
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    @Test
    @DisplayName("updateCategory - 존재하지 않는 카테고리 수정 시 새로 생성")
    void updateCategory_CategoryNotFoundCreatesNew() {
        // Given - 카테고리가 존재하지 않음
        when(valueOperations.get(KEY_PREFIX + "999")).thenReturn(null);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());

        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(999L)
                .parentId(null)
                .categoryName("신규 카테고리")
                .displayOrder(1)
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.updateCategory(event);

        // Then - 새로운 카테고리로 생성됨
        verify(valueOperations).set(eq(KEY_PREFIX + "999"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getCategoryId().equals(999L) &&
                        cache.getCategoryName().equals("신규 카테고리") &&
                        cache.getCreatedAt() == null;  // 기존 카테고리가 없었으므로 null
            }
            return false;
        }));
    }

    @Test
    @DisplayName("updateCategory - 부모 카테고리 변경 시 depth 재계산")
    void updateCategory_ParentChangedDepthRecalculated() {
        // Given - 기존 카테고리와 새 부모 카테고리 존재
        CategoryCache existingCategory = CategoryCache.builder()
                .categoryId(10L)
                .categoryName("스마트폰")
                .parentId(1L)
                .depth(1)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        CategoryCache newParentCategory = CategoryCache.builder()
                .categoryId(2L)
                .categoryName("가전제품")
                .depth(0)
                .build();

        when(valueOperations.get(KEY_PREFIX + "10")).thenReturn(existingCategory);
        when(valueOperations.get(KEY_PREFIX + "2")).thenReturn(newParentCategory);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of(KEY_PREFIX + "10", KEY_PREFIX + "2"));

        CategoryUpdatedEvent event = CategoryUpdatedEvent.builder()
                .categoryId(10L)
                .parentId(2L)  // 부모 변경
                .categoryName("스마트폰")
                .displayOrder(1)
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.updateCategory(event);

        // Then
        verify(valueOperations).set(eq(KEY_PREFIX + "10"), argThat(value -> {
            if (value instanceof CategoryCache) {
                CategoryCache cache = (CategoryCache) value;
                return cache.getParentId().equals(2L) &&
                        cache.getDepth().equals(1);  // 새 부모 depth(0) + 1
            }
            return false;
        }));
    }

    @Test
    @DisplayName("deleteCategory - 카테고리 삭제 이벤트 처리 성공")
    void deleteCategory_Success() {
        // Given
        when(redisTemplate.delete(KEY_PREFIX + "1")).thenReturn(true);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());

        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.deleteCategory(event);

        // Then
        verify(redisTemplate).delete(KEY_PREFIX + "1");
        verify(valueOperations).set(eq(KEY_INDEX_ALL), anyString());
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    @Test
    @DisplayName("deleteCategory - 존재하지 않는 카테고리 삭제 시 경고 로그만")
    void deleteCategory_CategoryNotFound() {
        // Given
        when(redisTemplate.delete(KEY_PREFIX + "999")).thenReturn(false);

        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(999L)
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.deleteCategory(event);

        // Then
        verify(redisTemplate).delete(KEY_PREFIX + "999");
        // index:all과 display:tree는 재구성되지 않음 (삭제 실패)
        verify(valueOperations, never()).set(eq(KEY_INDEX_ALL), anyString());
        verify(valueOperations, never()).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    @Test
    @DisplayName("deleteCategory - 모든 카테고리 삭제 시 빈 목록으로 저장")
    void deleteCategory_AllCategoriesDeleted() {
        // Given - 마지막 남은 카테고리 삭제
        when(redisTemplate.delete(KEY_PREFIX + "1")).thenReturn(true);
        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of());

        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.deleteCategory(event);

        // Then - 빈 배열로 저장됨
        verify(valueOperations).set(KEY_INDEX_ALL, "[]");
        verify(valueOperations).set(KEY_DISPLAY_TREE, "[]");
    }

    @Test
    @DisplayName("rebuildIndexAndTree - 개별 카테고리 키들로부터 index와 tree 재구성")
    void rebuildIndexAndTree_ReconstructFromIndividualKeys() {
        // Given - 여러 개별 카테고리 키가 존재
        CategoryCache category1 = CategoryCache.builder()
                .categoryId(1L)
                .categoryName("전자제품")
                .displayOrder(1)
                .depth(0)
                .build();

        CategoryCache category2 = CategoryCache.builder()
                .categoryId(2L)
                .parentId(1L)
                .categoryName("스마트폰")
                .displayOrder(1)
                .depth(1)
                .build();

        when(redisTemplate.keys(KEY_PREFIX + "[0-9]*")).thenReturn(Set.of(
                KEY_PREFIX + "1",
                KEY_PREFIX + "2"
        ));
        when(valueOperations.get(KEY_PREFIX + "1")).thenReturn(category1);
        when(valueOperations.get(KEY_PREFIX + "2")).thenReturn(category2);
        when(redisTemplate.delete(KEY_PREFIX + "1")).thenReturn(true);

        CategoryDeletedEvent event = CategoryDeletedEvent.builder()
                .categoryId(1L)
                .deletedAt(LocalDateTime.now())
                .build();

        // When
        categorySyncService.deleteCategory(event);

        // Then - index:all과 display:tree가 재구성됨
        verify(valueOperations).set(eq(KEY_INDEX_ALL), anyString());
        verify(valueOperations).set(eq(KEY_DISPLAY_TREE), anyString());
    }

    private CatalogSyncCategoryResponse createMockCategory(Long id, Long parentId, String name,
                                                          Integer displayOrder, Integer depth) {
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
            ReflectionTestUtils.setField(category, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(category, "updatedAt", LocalDateTime.now());
            return category;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock category", e);
        }
    }
}

package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncCategoryResponse;
import com.example.catalogservice.domain.CategoryCache;
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

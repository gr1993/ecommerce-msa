package com.example.catalogservice.controller;

import com.example.catalogservice.domain.CategoryCache;
import com.example.catalogservice.service.CategorySyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategorySyncController.class)
@ContextConfiguration(classes = {CategorySyncController.class, CategorySyncControllerTest.TestConfig.class})
@DisplayName("CategorySyncController 단위 테스트")
class CategorySyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategorySyncService categorySyncService;

    @BeforeEach
    void setUp() {
        Mockito.reset(categorySyncService);
    }

    @Configuration
    static class TestConfig {
        @Bean
        public CategorySyncService categorySyncService() {
            return Mockito.mock(CategorySyncService.class);
        }
    }

    @Test
    @DisplayName("POST /api/internal/sync/categories/full - 전체 동기화 성공")
    void fullSync_Success() throws Exception {
        // Given
        int syncedCount = 10;
        when(categorySyncService.fullSync()).thenReturn(syncedCount);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/categories/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount", is(syncedCount)))
                .andExpect(jsonPath("$.message", is("Category full sync completed successfully")));

        verify(categorySyncService).fullSync();
    }

    @Test
    @DisplayName("POST /api/internal/sync/categories/full - 동기화 대상 없음")
    void fullSync_NoCategories() throws Exception {
        // Given
        when(categorySyncService.fullSync()).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/categories/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount", is(0)))
                .andExpect(jsonPath("$.message", is("Category full sync completed successfully")));
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories - 전체 카테고리 조회 성공")
    void getAllCategories_Success() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<CategoryCache> categories = List.of(
                CategoryCache.builder()
                        .categoryId(1L)
                        .parentId(null)
                        .categoryName("전자제품")
                        .displayOrder(1)
                        .depth(0)
                        .createdAt(now)
                        .updatedAt(now)
                        .build(),
                CategoryCache.builder()
                        .categoryId(2L)
                        .parentId(1L)
                        .categoryName("스마트폰")
                        .displayOrder(1)
                        .depth(1)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()
        );

        when(categorySyncService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].categoryId", is(1)))
                .andExpect(jsonPath("$[0].categoryName", is("전자제품")))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].displayOrder", is(1)))
                .andExpect(jsonPath("$[0].depth", is(0)))
                .andExpect(jsonPath("$[1].categoryId", is(2)))
                .andExpect(jsonPath("$[1].categoryName", is("스마트폰")))
                .andExpect(jsonPath("$[1].parentId", is(1)))
                .andExpect(jsonPath("$[1].displayOrder", is(1)))
                .andExpect(jsonPath("$[1].depth", is(1)));

        verify(categorySyncService).getAllCategories();
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories - 카테고리 없음")
    void getAllCategories_Empty() throws Exception {
        // Given
        when(categorySyncService.getAllCategories()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories/{categoryId} - 단일 카테고리 조회 성공")
    void getCategoryById_Success() throws Exception {
        // Given
        Long categoryId = 100L;
        LocalDateTime now = LocalDateTime.now();
        CategoryCache category = CategoryCache.builder()
                .categoryId(categoryId)
                .parentId(10L)
                .categoryName("테스트 카테고리")
                .displayOrder(5)
                .depth(2)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(categorySyncService.getCategoryById(categoryId)).thenReturn(category);

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", is(categoryId.intValue())))
                .andExpect(jsonPath("$.parentId", is(10)))
                .andExpect(jsonPath("$.categoryName", is("테스트 카테고리")))
                .andExpect(jsonPath("$.displayOrder", is(5)))
                .andExpect(jsonPath("$.depth", is(2)));

        verify(categorySyncService).getCategoryById(categoryId);
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories/{categoryId} - 카테고리 없음 404")
    void getCategoryById_NotFound() throws Exception {
        // Given
        Long categoryId = 999L;
        when(categorySyncService.getCategoryById(categoryId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(categorySyncService).getCategoryById(categoryId);
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories/{categoryId} - 루트 카테고리 조회")
    void getCategoryById_RootCategory() throws Exception {
        // Given
        Long categoryId = 1L;
        CategoryCache rootCategory = CategoryCache.builder()
                .categoryId(categoryId)
                .parentId(null)
                .categoryName("루트 카테고리")
                .displayOrder(1)
                .depth(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categorySyncService.getCategoryById(categoryId)).thenReturn(rootCategory);

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId", is(1)))
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.categoryName", is("루트 카테고리")))
                .andExpect(jsonPath("$.depth", is(0)));
    }

    @Test
    @DisplayName("GET /api/internal/sync/categories - 다중 depth 카테고리 계층 구조")
    void getAllCategories_MultipleDepths() throws Exception {
        // Given
        List<CategoryCache> categories = List.of(
                CategoryCache.builder()
                        .categoryId(1L)
                        .parentId(null)
                        .categoryName("전자제품")
                        .displayOrder(1)
                        .depth(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                CategoryCache.builder()
                        .categoryId(2L)
                        .parentId(1L)
                        .categoryName("스마트폰")
                        .displayOrder(1)
                        .depth(1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                CategoryCache.builder()
                        .categoryId(3L)
                        .parentId(2L)
                        .categoryName("삼성")
                        .displayOrder(1)
                        .depth(2)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        when(categorySyncService.getAllCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/internal/sync/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].depth", is(0)))
                .andExpect(jsonPath("$[1].depth", is(1)))
                .andExpect(jsonPath("$[2].depth", is(2)))
                .andExpect(jsonPath("$[2].parentId", is(2)));
    }

    @Test
    @DisplayName("POST /api/internal/sync/categories/full - 대량 동기화")
    void fullSync_LargeDataset() throws Exception {
        // Given
        int syncedCount = 1000;
        when(categorySyncService.fullSync()).thenReturn(syncedCount);

        // When & Then
        mockMvc.perform(post("/api/internal/sync/categories/full")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncedCount", is(syncedCount)))
                .andExpect(jsonPath("$.message", is("Category full sync completed successfully")));
    }
}

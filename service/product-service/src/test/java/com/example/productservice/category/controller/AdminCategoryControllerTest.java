package com.example.productservice.category.controller;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.dto.CategoryUpdateRequest;
import com.example.productservice.category.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCategoryController.class)
@DisplayName("AdminCategoryController 테스트")
@ActiveProfiles("test")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryResponse = CategoryResponse.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("카테고리 등록 - 성공")
    void createCategory_success() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenReturn(categoryResponse);

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(1L))
                .andExpect(jsonPath("$.categoryName").value("의류"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.isDisplayed").value(true));

        verify(categoryService, times(1)).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 하위 카테고리 성공")
    void createCategory_withParent_success() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(1L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        CategoryResponse childResponse = CategoryResponse.builder()
                .categoryId(2L)
                .parentId(1L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenReturn(childResponse);

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(2L))
                .andExpect(jsonPath("$.parentId").value(1L))
                .andExpect(jsonPath("$.categoryName").value("상의"));

        verify(categoryService, times(1)).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 카테고리명 누락")
    void createCategory_missingCategoryName() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 빈 카테고리명")
    void createCategory_emptyCategoryName() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 존재하지 않는 상위 카테고리")
    void createCategory_parentNotFound() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .parentId(999L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenThrow(new IllegalArgumentException("상위 카테고리를 찾을 수 없습니다. parentId: 999"));

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(categoryService, times(1)).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 기본값으로 등록")
    void createCategory_withDefaults() throws Exception {
        // given
        String jsonRequest = "{\"categoryName\": \"신발\"}";

        CategoryResponse defaultResponse = CategoryResponse.builder()
                .categoryId(3L)
                .categoryName("신발")
                .displayOrder(0)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenReturn(defaultResponse);

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("신발"))
                .andExpect(jsonPath("$.displayOrder").value(0))
                .andExpect(jsonPath("$.isDisplayed").value(true));

        verify(categoryService, times(1)).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 등록 - 전시 안함으로 등록")
    void createCategory_notDisplayed() throws Exception {
        // given
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .categoryName("비공개")
                .displayOrder(99)
                .isDisplayed(false)
                .build();

        CategoryResponse hiddenResponse = CategoryResponse.builder()
                .categoryId(4L)
                .categoryName("비공개")
                .displayOrder(99)
                .isDisplayed(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.createCategory(any(CategoryCreateRequest.class)))
                .thenReturn(hiddenResponse);

        // when & then
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDisplayed").value(false))
                .andExpect(jsonPath("$.displayOrder").value(99));

        verify(categoryService, times(1)).createCategory(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 성공")
    void getCategoryTree_success() throws Exception {
        // given
        CategoryTreeResponse child = CategoryTreeResponse.builder()
                .categoryId(2L)
                .parentId(1L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .depth(2)
                .children(null)
                .build();

        CategoryTreeResponse root = CategoryTreeResponse.builder()
                .categoryId(1L)
                .parentId(null)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .depth(1)
                .children(Collections.singletonList(child))
                .build();

        when(categoryService.getCategoryTree()).thenReturn(Collections.singletonList(root));

        // when & then
        mockMvc.perform(get("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(1L))
                .andExpect(jsonPath("$[0].categoryName").value("의류"))
                .andExpect(jsonPath("$[0].depth").value(1))
                .andExpect(jsonPath("$[0].children").isArray())
                .andExpect(jsonPath("$[0].children.length()").value(1))
                .andExpect(jsonPath("$[0].children[0].categoryId").value(2L))
                .andExpect(jsonPath("$[0].children[0].categoryName").value("상의"))
                .andExpect(jsonPath("$[0].children[0].depth").value(2));

        verify(categoryService, times(1)).getCategoryTree();
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 빈 목록")
    void getCategoryTree_empty() throws Exception {
        // given
        when(categoryService.getCategoryTree()).thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService, times(1)).getCategoryTree();
    }

    @Test
    @DisplayName("카테고리 트리 조회 - 다중 최상위 카테고리")
    void getCategoryTree_multipleRoots() throws Exception {
        // given
        CategoryTreeResponse root1 = CategoryTreeResponse.builder()
                .categoryId(1L)
                .categoryName("의류")
                .displayOrder(1)
                .isDisplayed(true)
                .depth(1)
                .build();

        CategoryTreeResponse root2 = CategoryTreeResponse.builder()
                .categoryId(2L)
                .categoryName("신발")
                .displayOrder(2)
                .isDisplayed(true)
                .depth(1)
                .build();

        when(categoryService.getCategoryTree()).thenReturn(Arrays.asList(root1, root2));

        // when & then
        mockMvc.perform(get("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("의류"))
                .andExpect(jsonPath("$[1].categoryName").value("신발"));

        verify(categoryService, times(1)).getCategoryTree();
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 성공")
    void getCategory_success() throws Exception {
        // given
        when(categoryService.getCategory(1L)).thenReturn(categoryResponse);

        // when & then
        mockMvc.perform(get("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1L))
                .andExpect(jsonPath("$.categoryName").value("의류"))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.isDisplayed").value(true));

        verify(categoryService, times(1)).getCategory(1L);
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 하위 카테고리")
    void getCategory_childCategory() throws Exception {
        // given
        CategoryResponse childResponse = CategoryResponse.builder()
                .categoryId(2L)
                .parentId(1L)
                .categoryName("상의")
                .displayOrder(1)
                .isDisplayed(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.getCategory(2L)).thenReturn(childResponse);

        // when & then
        mockMvc.perform(get("/api/admin/categories/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(2L))
                .andExpect(jsonPath("$.parentId").value(1L))
                .andExpect(jsonPath("$.categoryName").value("상의"));

        verify(categoryService, times(1)).getCategory(2L);
    }

    @Test
    @DisplayName("카테고리 상세 조회 - 존재하지 않는 카테고리")
    void getCategory_notFound() throws Exception {
        // given
        when(categoryService.getCategory(999L))
                .thenThrow(new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: 999"));

        // when & then
        mockMvc.perform(get("/api/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).getCategory(999L);
    }

    @Test
    @DisplayName("카테고리 수정 - 성공")
    void updateCategory_success() throws Exception {
        // given
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .categoryName("의류(수정)")
                .displayOrder(10)
                .isDisplayed(false)
                .build();

        CategoryResponse updatedResponse = CategoryResponse.builder()
                .categoryId(1L)
                .categoryName("의류(수정)")
                .displayOrder(10)
                .isDisplayed(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(categoryService.updateCategory(eq(1L), any(CategoryUpdateRequest.class)))
                .thenReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1L))
                .andExpect(jsonPath("$.categoryName").value("의류(수정)"))
                .andExpect(jsonPath("$.displayOrder").value(10))
                .andExpect(jsonPath("$.isDisplayed").value(false));

        verify(categoryService, times(1)).updateCategory(eq(1L), any(CategoryUpdateRequest.class));
    }

    @Test
    @DisplayName("카테고리 수정 - 카테고리명 누락")
    void updateCategory_missingCategoryName() throws Exception {
        // given
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .displayOrder(10)
                .isDisplayed(true)
                .build();

        // when & then
        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).updateCategory(anyLong(), any(CategoryUpdateRequest.class));
    }

    @Test
    @DisplayName("카테고리 수정 - 존재하지 않는 카테고리")
    void updateCategory_notFound() throws Exception {
        // given
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .categoryName("수정된 카테고리")
                .build();

        when(categoryService.updateCategory(eq(999L), any(CategoryUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: 999"));

        // when & then
        mockMvc.perform(put("/api/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).updateCategory(eq(999L), any(CategoryUpdateRequest.class));
    }

    @Test
    @DisplayName("카테고리 삭제 - 성공")
    void deleteCategory_success() throws Exception {
        // given
        doNothing().when(categoryService).deleteCategory(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    @DisplayName("카테고리 삭제 - 존재하지 않는 카테고리")
    void deleteCategory_notFound() throws Exception {
        // given
        doThrow(new IllegalArgumentException("카테고리를 찾을 수 없습니다. categoryId: 999"))
                .when(categoryService).deleteCategory(999L);

        // when & then
        mockMvc.perform(delete("/api/admin/categories/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(categoryService, times(1)).deleteCategory(999L);
    }

    @Test
    @DisplayName("카테고리 삭제 - 하위 카테고리 존재")
    void deleteCategory_hasChildren() throws Exception {
        // given
        doThrow(new IllegalStateException("하위 카테고리가 존재하여 삭제할 수 없습니다. categoryId: 1"))
                .when(categoryService).deleteCategory(1L);

        // when & then
        mockMvc.perform(delete("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(categoryService, times(1)).deleteCategory(1L);
    }
}

package com.example.productservice.category.controller;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}

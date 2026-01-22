package com.example.catalogservice.controller;

import com.example.catalogservice.domain.CategoryTreeNode;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@ContextConfiguration(classes = {CategoryController.class, CategoryControllerTest.TestConfig.class})
@DisplayName("CategoryController 단위 테스트")
class CategoryControllerTest {

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
    @DisplayName("GET /api/catalog/categories/tree - 트리 조회 성공")
    void getCategoryTree_Success() throws Exception {
        // Given
        List<CategoryTreeNode> tree = List.of(
                CategoryTreeNode.builder()
                        .categoryId(1L)
                        .parentId(null)
                        .categoryName("전자제품")
                        .displayOrder(1)
                        .depth(0)
                        .children(List.of(
                                CategoryTreeNode.builder()
                                        .categoryId(2L)
                                        .parentId(1L)
                                        .categoryName("스마트폰")
                                        .displayOrder(1)
                                        .depth(1)
                                        .children(List.of())
                                        .build()
                        ))
                        .build(),
                CategoryTreeNode.builder()
                        .categoryId(3L)
                        .parentId(null)
                        .categoryName("의류")
                        .displayOrder(2)
                        .depth(0)
                        .children(List.of())
                        .build()
        );

        when(categorySyncService.getCategoryTree()).thenReturn(tree);

        // When & Then
        mockMvc.perform(get("/api/catalog/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].categoryId", is(1)))
                .andExpect(jsonPath("$[0].categoryName", is("전자제품")))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[0].displayOrder", is(1)))
                .andExpect(jsonPath("$[0].depth", is(0)))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].categoryId", is(2)))
                .andExpect(jsonPath("$[0].children[0].categoryName", is("스마트폰")))
                .andExpect(jsonPath("$[0].children[0].parentId", is(1)))
                .andExpect(jsonPath("$[1].categoryId", is(3)))
                .andExpect(jsonPath("$[1].categoryName", is("의류")))
                .andExpect(jsonPath("$[1].children", hasSize(0)));

        verify(categorySyncService).getCategoryTree();
    }

    @Test
    @DisplayName("GET /api/catalog/categories/tree - 빈 트리인 경우 200 OK와 빈 배열 반환")
    void getCategoryTree_EmptyTree() throws Exception {
        // Given
        when(categorySyncService.getCategoryTree()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/catalog/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(categorySyncService).getCategoryTree();
    }

    @Test
    @DisplayName("GET /api/catalog/categories/tree - 다층 계층 구조 트리")
    void getCategoryTree_MultiLevel() throws Exception {
        // Given
        List<CategoryTreeNode> tree = List.of(
                CategoryTreeNode.builder()
                        .categoryId(1L)
                        .parentId(null)
                        .categoryName("전자제품")
                        .displayOrder(1)
                        .depth(0)
                        .children(List.of(
                                CategoryTreeNode.builder()
                                        .categoryId(2L)
                                        .parentId(1L)
                                        .categoryName("스마트폰")
                                        .displayOrder(1)
                                        .depth(1)
                                        .children(List.of(
                                                CategoryTreeNode.builder()
                                                        .categoryId(3L)
                                                        .parentId(2L)
                                                        .categoryName("삼성")
                                                        .displayOrder(1)
                                                        .depth(2)
                                                        .children(List.of())
                                                        .build(),
                                                CategoryTreeNode.builder()
                                                        .categoryId(4L)
                                                        .parentId(2L)
                                                        .categoryName("애플")
                                                        .displayOrder(2)
                                                        .depth(2)
                                                        .children(List.of())
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );

        when(categorySyncService.getCategoryTree()).thenReturn(tree);

        // When & Then
        mockMvc.perform(get("/api/catalog/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryId", is(1)))
                .andExpect(jsonPath("$[0].categoryName", is("전자제품")))
                .andExpect(jsonPath("$[0].depth", is(0)))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].categoryId", is(2)))
                .andExpect(jsonPath("$[0].children[0].categoryName", is("스마트폰")))
                .andExpect(jsonPath("$[0].children[0].depth", is(1)))
                .andExpect(jsonPath("$[0].children[0].children", hasSize(2)))
                .andExpect(jsonPath("$[0].children[0].children[0].categoryId", is(3)))
                .andExpect(jsonPath("$[0].children[0].children[0].categoryName", is("삼성")))
                .andExpect(jsonPath("$[0].children[0].children[0].depth", is(2)))
                .andExpect(jsonPath("$[0].children[0].children[1].categoryId", is(4)))
                .andExpect(jsonPath("$[0].children[0].children[1].categoryName", is("애플")))
                .andExpect(jsonPath("$[0].children[0].children[1].depth", is(2)));

        verify(categorySyncService).getCategoryTree();
    }

    @Test
    @DisplayName("GET /api/catalog/categories/tree - 여러 루트 카테고리")
    void getCategoryTree_MultipleRoots() throws Exception {
        // Given
        List<CategoryTreeNode> tree = List.of(
                CategoryTreeNode.builder()
                        .categoryId(1L)
                        .parentId(null)
                        .categoryName("전자제품")
                        .displayOrder(1)
                        .depth(0)
                        .children(List.of())
                        .build(),
                CategoryTreeNode.builder()
                        .categoryId(2L)
                        .parentId(null)
                        .categoryName("의류")
                        .displayOrder(2)
                        .depth(0)
                        .children(List.of())
                        .build(),
                CategoryTreeNode.builder()
                        .categoryId(3L)
                        .parentId(null)
                        .categoryName("식품")
                        .displayOrder(3)
                        .depth(0)
                        .children(List.of())
                        .build()
        );

        when(categorySyncService.getCategoryTree()).thenReturn(tree);

        // When & Then
        mockMvc.perform(get("/api/catalog/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].categoryId", is(1)))
                .andExpect(jsonPath("$[0].categoryName", is("전자제품")))
                .andExpect(jsonPath("$[0].depth", is(0)))
                .andExpect(jsonPath("$[1].categoryId", is(2)))
                .andExpect(jsonPath("$[1].categoryName", is("의류")))
                .andExpect(jsonPath("$[1].depth", is(0)))
                .andExpect(jsonPath("$[2].categoryId", is(3)))
                .andExpect(jsonPath("$[2].categoryName", is("식품")))
                .andExpect(jsonPath("$[2].depth", is(0)));

        verify(categorySyncService).getCategoryTree();
    }
}

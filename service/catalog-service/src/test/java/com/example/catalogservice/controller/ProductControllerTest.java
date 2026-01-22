package com.example.catalogservice.controller;

import com.example.catalogservice.controller.dto.ProductResponse;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.service.ProductSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProductController.class)
@ContextConfiguration(classes = {ProductController.class, ProductControllerTest.TestConfig.class})
@DisplayName("ProductController 단위 테스트")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductSearchService productSearchService;

    @BeforeEach
    void setUp() {
        Mockito.reset(productSearchService);
    }

    @Configuration
    static class TestConfig {
        @Bean
        public ProductSearchService productSearchService() {
            return Mockito.mock(ProductSearchService.class);
        }
    }

    @Test
    @DisplayName("GET /api/catalog/products - 상품 목록 조회 성공")
    void getProducts_Success() throws Exception {
        // Given
        Long categoryId = 100L;
        Pageable pageable = PageRequest.of(0, 20);

        List<ProductDocument> documents = List.of(
                createProductDocument("1", "노트북", List.of(100L, 200L)),
                createProductDocument("2", "마우스", List.of(100L, 300L)),
                createProductDocument("3", "키보드", List.of(100L, 400L))
        );
        Page<ProductDocument> productsPage = new PageImpl<>(documents, pageable, documents.size());

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].productId", is("1")))
                .andExpect(jsonPath("$.content[0].productName", is("노트북")))
                .andExpect(jsonPath("$.content[0].basePrice", is(10000)))
                .andExpect(jsonPath("$.content[0].salePrice", is(8000)))
                .andExpect(jsonPath("$.content[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$.content[0].categoryIds", hasSize(2)))
                .andExpect(jsonPath("$.content[1].productId", is("2")))
                .andExpect(jsonPath("$.content[1].productName", is("마우스")))
                .andExpect(jsonPath("$.content[2].productId", is("3")))
                .andExpect(jsonPath("$.content[2].productName", is("키보드")))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.number", is(0)));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 빈 결과 반환")
    void getProducts_EmptyResult() throws Exception {
        // Given
        Long categoryId = 999L;
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductDocument> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 페이지네이션 파라미터 적용")
    void getProducts_WithPagination() throws Exception {
        // Given
        Long categoryId = 100L;
        Pageable pageable = PageRequest.of(2, 5);

        List<ProductDocument> documents = List.of(
                createProductDocument("11", "상품11", List.of(100L)),
                createProductDocument("12", "상품12", List.of(100L)),
                createProductDocument("13", "상품13", List.of(100L))
        );
        Page<ProductDocument> productsPage = new PageImpl<>(documents, pageable, 13);

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .param("page", "2")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(13)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(2)));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 기본 페이지 크기 20 적용")
    void getProducts_DefaultPageSize() throws Exception {
        // Given
        Long categoryId = 100L;
        Pageable pageable = PageRequest.of(0, 20);

        Page<ProductDocument> productsPage = new PageImpl<>(List.of(), pageable, 0);

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.number", is(0)));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - categoryId 누락 시 전체 상품 조회 (null 전달)")
    void getProducts_MissingCategoryId_AllProducts() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(0, 20);

        List<ProductDocument> documents = List.of(
                createProductDocument("1", "노트북", List.of(100L)),
                createProductDocument("2", "마우스", List.of(200L)),
                createProductDocument("3", "키보드", List.of(300L)),
                createProductDocument("4", "모니터", List.of(400L))
        );
        Page<ProductDocument> productsPage = new PageImpl<>(documents, pageable, documents.size());

        when(productSearchService.searchProducts(eq(null), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.totalElements", is(4)))
                .andExpect(jsonPath("$.content[0].productName", is("노트북")))
                .andExpect(jsonPath("$.content[1].productName", is("마우스")))
                .andExpect(jsonPath("$.content[2].productName", is("키보드")))
                .andExpect(jsonPath("$.content[3].productName", is("모니터")));

        verify(productSearchService).searchProducts(eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - categoryId 없이 페이지네이션만 적용")
    void getProducts_NoCategoryWithPagination() throws Exception {
        // Given
        Pageable pageable = PageRequest.of(1, 5);

        List<ProductDocument> documents = List.of(
                createProductDocument("6", "상품6", List.of(100L)),
                createProductDocument("7", "상품7", List.of(200L))
        );
        Page<ProductDocument> productsPage = new PageImpl<>(documents, pageable, 12);

        when(productSearchService.searchProducts(eq(null), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(12)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(1)));

        verify(productSearchService).searchProducts(eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - ProductResponse 매핑 검증")
    void getProducts_ResponseMapping() throws Exception {
        // Given
        Long categoryId = 100L;
        Pageable pageable = PageRequest.of(0, 20);

        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 15, 30);

        ProductDocument document = ProductDocument.builder()
                .productId("999")
                .productName("테스트 상품")
                .description("상품 설명")
                .basePrice(50000L)
                .salePrice(45000L)
                .status("ON_SALE")
                .primaryImageUrl("https://example.com/test.jpg")
                .categoryIds(List.of(100L, 200L, 300L))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        Page<ProductDocument> productsPage = new PageImpl<>(List.of(document), pageable, 1);

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].productId", is("999")))
                .andExpect(jsonPath("$.content[0].productName", is("테스트 상품")))
                .andExpect(jsonPath("$.content[0].description", is("상품 설명")))
                .andExpect(jsonPath("$.content[0].basePrice", is(50000)))
                .andExpect(jsonPath("$.content[0].salePrice", is(45000)))
                .andExpect(jsonPath("$.content[0].status", is("ON_SALE")))
                .andExpect(jsonPath("$.content[0].primaryImageUrl", is("https://example.com/test.jpg")))
                .andExpect(jsonPath("$.content[0].categoryIds", hasSize(3)))
                .andExpect(jsonPath("$.content[0].categoryIds[0]", is(100)))
                .andExpect(jsonPath("$.content[0].categoryIds[1]", is(200)))
                .andExpect(jsonPath("$.content[0].categoryIds[2]", is(300)))
                .andExpect(jsonPath("$.content[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.content[0].updatedAt", notNullValue()));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 대량 데이터 조회")
    void getProducts_LargeDataSet() throws Exception {
        // Given
        Long categoryId = 100L;
        Pageable pageable = PageRequest.of(0, 20);

        List<ProductDocument> documents = List.of(
                createProductDocument("1", "상품1", List.of(100L)),
                createProductDocument("2", "상품2", List.of(100L)),
                createProductDocument("3", "상품3", List.of(100L)),
                createProductDocument("4", "상품4", List.of(100L)),
                createProductDocument("5", "상품5", List.of(100L)),
                createProductDocument("6", "상품6", List.of(100L)),
                createProductDocument("7", "상품7", List.of(100L)),
                createProductDocument("8", "상품8", List.of(100L)),
                createProductDocument("9", "상품9", List.of(100L)),
                createProductDocument("10", "상품10", List.of(100L)),
                createProductDocument("11", "상품11", List.of(100L)),
                createProductDocument("12", "상품12", List.of(100L)),
                createProductDocument("13", "상품13", List.of(100L)),
                createProductDocument("14", "상품14", List.of(100L)),
                createProductDocument("15", "상품15", List.of(100L)),
                createProductDocument("16", "상품16", List.of(100L)),
                createProductDocument("17", "상품17", List.of(100L)),
                createProductDocument("18", "상품18", List.of(100L)),
                createProductDocument("19", "상품19", List.of(100L)),
                createProductDocument("20", "상품20", List.of(100L))
        );
        Page<ProductDocument> productsPage = new PageImpl<>(documents, pageable, 100);

        when(productSearchService.searchProducts(eq(categoryId), any(Pageable.class)))
                .thenReturn(productsPage);

        // When & Then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", categoryId.toString())
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(20)))
                .andExpect(jsonPath("$.totalElements", is(100)))
                .andExpect(jsonPath("$.totalPages", is(5)));

        verify(productSearchService).searchProducts(eq(categoryId), any(Pageable.class));
    }

    private ProductDocument createProductDocument(String id, String name, List<Long> categoryIds) {
        return ProductDocument.builder()
                .productId(id)
                .productName(name)
                .description("상품 설명 " + name)
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/image.jpg")
                .categoryIds(categoryIds)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

package com.example.catalogservice.controller;

import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.service.ProductSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("GET /api/catalog/products - 조건 없이 전체 조회")
    void getProducts_withoutConditions() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로", 3300000L),
                createProduct("2", "갤럭시 S24", 1400000L)
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].productId").value("1"))
                .andExpect(jsonPath("$.content[0].productName").value("맥북 프로"))
                .andExpect(jsonPath("$.content[1].productId").value("2"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?productName=맥북 - 상품명으로 검색")
    void getProducts_byProductName() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로 16인치", 3300000L),
                createProduct("7", "맥북 에어 M2", 1400000L)
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("productName", "맥북"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].productName").value("맥북 프로 16인치"))
                .andExpect(jsonPath("$.content[1].productName").value("맥북 에어 M2"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?categoryId=1 - 카테고리로 필터링")
    void getProducts_byCategoryId() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로", 3300000L, List.of(1L, 10L)),
                createProduct("3", "아이패드 프로", 1100000L, List.of(1L, 11L))
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].categoryIds[0]").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?minPrice=1000000&maxPrice=2000000 - 가격 범위 필터링")
    void getProducts_byPriceRange() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("2", "갤럭시 S24", 1400000L),
                createProduct("7", "맥북 에어 M2", 1400000L)
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("minPrice", "1000000")
                        .param("maxPrice", "2000000"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?status=ACTIVE - 상태로 필터링")
    void getProducts_byStatus() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로", 3300000L, "ACTIVE"),
                createProduct("2", "갤럭시 S24", 1400000L, "ACTIVE")
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?productName=맥북&categoryId=1 - 복합 조건 검색")
    void getProducts_combinedConditions() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로 16인치", 3300000L, List.of(1L, 10L)),
                createProduct("7", "맥북 에어 M2", 1400000L, List.of(1L, 10L))
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("productName", "맥북")
                        .param("categoryId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?page=1&size=10 - 페이지네이션")
    void getProducts_withPagination() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("11", "상품 11", 100000L),
                createProduct("12", "상품 12", 100000L)
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                22
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(22))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products?sort=salePrice,asc - 정렬 옵션")
    void getProducts_withSorting() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("4", "에어팟 프로", 320000L),
                createProduct("6", "갤럭시 탭 S9", 850000L)
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "salePrice")),
                2
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("sort", "salePrice,asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].salePrice").value(320000))
                .andExpect(jsonPath("$.content[1].salePrice").value(850000));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 검색 결과 없음")
    void getProducts_noResults() throws Exception {
        // given
        PageImpl<ProductDocument> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                0
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("productName", "존재하지않는상품"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    @Test
    @DisplayName("GET /api/catalog/products - 모든 파라미터 조합 테스트")
    void getProducts_allParameters() throws Exception {
        // given
        List<ProductDocument> products = List.of(
                createProduct("1", "맥북 프로", 3300000L, "ACTIVE", List.of(1L, 10L))
        );
        PageImpl<ProductDocument> page = new PageImpl<>(
                products,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "salePrice")),
                1
        );

        given(productSearchService.searchProducts(any(ProductSearchRequest.class)))
                .willReturn(page);

        // when & then
        mockMvc.perform(get("/api/catalog/products")
                        .param("productName", "맥북")
                        .param("categoryId", "1")
                        .param("status", "ACTIVE")
                        .param("minPrice", "3000000")
                        .param("maxPrice", "4000000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "salePrice,asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("맥북 프로"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(productSearchService).searchProducts(any(ProductSearchRequest.class));
    }

    private ProductDocument createProduct(String id, String name, Long salePrice) {
        return createProduct(id, name, salePrice, "ACTIVE");
    }

    private ProductDocument createProduct(String id, String name, Long salePrice, String status) {
        return createProduct(id, name, salePrice, status, List.of(1L));
    }

    private ProductDocument createProduct(String id, String name, Long salePrice, List<Long> categoryIds) {
        return createProduct(id, name, salePrice, "ACTIVE", categoryIds);
    }

    private ProductDocument createProduct(
            String id,
            String name,
            Long salePrice,
            String status,
            List<Long> categoryIds
    ) {
        return ProductDocument.builder()
                .productId(id)
                .productName(name)
                .description("상품 설명")
                .basePrice(salePrice + 100000L)
                .salePrice(salePrice)
                .status(status)
                .primaryImageUrl("https://example.com/image.jpg")
                .categoryIds(categoryIds)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

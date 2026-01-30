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

    @Test
    @DisplayName("GET /api/catalog/products/autocomplete - 상품명 자동완성 성공")
    void autocompleteProductName_success() throws Exception {
        // given
        String keyword = "노트북";
        List<String> suggestions = List.of(
                "삼성 노트북",
                "LG 노트북",
                "애플 노트북 프로",
                "HP 노트북",
                "레노버 노트북"
        );

        given(productSearchService.autocompleteProductName(keyword))
                .willReturn(suggestions);

        // when & then
        mockMvc.perform(get("/api/catalog/products/autocomplete")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("삼성 노트북"))
                .andExpect(jsonPath("$[1]").value("LG 노트북"))
                .andExpect(jsonPath("$[2]").value("애플 노트북 프로"))
                .andExpect(jsonPath("$[3]").value("HP 노트북"))
                .andExpect(jsonPath("$[4]").value("레노버 노트북"));

        verify(productSearchService).autocompleteProductName(keyword);
    }

    @Test
    @DisplayName("GET /api/catalog/products/autocomplete - 3개 미만 결과")
    void autocompleteProductName_lessThanFiveResults() throws Exception {
        // given
        String keyword = "아이패드";
        List<String> suggestions = List.of(
                "아이패드 프로",
                "아이패드 미니",
                "아이패드 에어"
        );

        given(productSearchService.autocompleteProductName(keyword))
                .willReturn(suggestions);

        // when & then
        mockMvc.perform(get("/api/catalog/products/autocomplete")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("아이패드 프로"))
                .andExpect(jsonPath("$[1]").value("아이패드 미니"))
                .andExpect(jsonPath("$[2]").value("아이패드 에어"));

        verify(productSearchService).autocompleteProductName(keyword);
    }

    @Test
    @DisplayName("GET /api/catalog/products/autocomplete - 검색 결과 없음")
    void autocompleteProductName_noResults() throws Exception {
        // given
        String keyword = "존재하지않는상품";
        List<String> suggestions = List.of();

        given(productSearchService.autocompleteProductName(keyword))
                .willReturn(suggestions);

        // when & then
        mockMvc.perform(get("/api/catalog/products/autocomplete")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productSearchService).autocompleteProductName(keyword);
    }

    @Test
    @DisplayName("GET /api/catalog/products/autocomplete - 빈 키워드")
    void autocompleteProductName_emptyKeyword() throws Exception {
        // given
        String keyword = "";
        List<String> suggestions = List.of();

        given(productSearchService.autocompleteProductName(keyword))
                .willReturn(suggestions);

        // when & then
        mockMvc.perform(get("/api/catalog/products/autocomplete")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(productSearchService).autocompleteProductName(keyword);
    }

    @Test
    @DisplayName("GET /api/catalog/products/autocomplete - 한글 키워드")
    void autocompleteProductName_koreanKeyword() throws Exception {
        // given
        String keyword = "갤럭시";
        List<String> suggestions = List.of(
                "갤럭시 S24",
                "갤럭시 Z Fold",
                "갤럭시 탭"
        );

        given(productSearchService.autocompleteProductName(keyword))
                .willReturn(suggestions);

        // when & then
        mockMvc.perform(get("/api/catalog/products/autocomplete")
                        .param("keyword", keyword))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("갤럭시 S24"))
                .andExpect(jsonPath("$[1]").value("갤럭시 Z Fold"))
                .andExpect(jsonPath("$[2]").value("갤럭시 탭"));

        verify(productSearchService).autocompleteProductName(keyword);
    }

    @Test
    @DisplayName("GET /api/catalog/products/{productId} - 상품 상세 조회 성공")
    void getProductDetail_success() throws Exception {
        // given
        String productId = "1";
        ProductDocument product = ProductDocument.builder()
                .productId(productId)
                .productName("맥북 프로 16인치")
                .description("M3 Max 칩이 탑재된 맥북 프로")
                .basePrice(4500000L)
                .salePrice(4300000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/macbook.jpg")
                .categoryIds(List.of(1L, 10L))
                .searchKeywords(List.of("맥북", "노트북", "애플"))
                .skus(List.of(
                        ProductDocument.SkuInfo.builder()
                                .skuId(1L)
                                .skuCode("MACBOOK-PRO-16-M3-SILVER")
                                .price(4300000L)
                                .stockQty(10)
                                .status("ACTIVE")
                                .build(),
                        ProductDocument.SkuInfo.builder()
                                .skuId(2L)
                                .skuCode("MACBOOK-PRO-16-M3-GRAY")
                                .price(4300000L)
                                .stockQty(5)
                                .status("ACTIVE")
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(productSearchService.findProductById(productId))
                .willReturn(product);

        // when & then
        mockMvc.perform(get("/api/catalog/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("맥북 프로 16인치"))
                .andExpect(jsonPath("$.description").value("M3 Max 칩이 탑재된 맥북 프로"))
                .andExpect(jsonPath("$.basePrice").value(4500000))
                .andExpect(jsonPath("$.salePrice").value(4300000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.primaryImageUrl").value("https://example.com/macbook.jpg"))
                .andExpect(jsonPath("$.categoryIds").isArray())
                .andExpect(jsonPath("$.categoryIds.length()").value(2))
                .andExpect(jsonPath("$.categoryIds[0]").value(1))
                .andExpect(jsonPath("$.categoryIds[1]").value(10))
                .andExpect(jsonPath("$.searchKeywords").isArray())
                .andExpect(jsonPath("$.searchKeywords.length()").value(3))
                .andExpect(jsonPath("$.searchKeywords[0]").value("맥북"))
                .andExpect(jsonPath("$.searchKeywords[1]").value("노트북"))
                .andExpect(jsonPath("$.searchKeywords[2]").value("애플"))
                .andExpect(jsonPath("$.skus").isArray())
                .andExpect(jsonPath("$.skus.length()").value(2))
                .andExpect(jsonPath("$.skus[0].skuId").value(1))
                .andExpect(jsonPath("$.skus[0].skuCode").value("MACBOOK-PRO-16-M3-SILVER"))
                .andExpect(jsonPath("$.skus[0].price").value(4300000))
                .andExpect(jsonPath("$.skus[0].stockQty").value(10))
                .andExpect(jsonPath("$.skus[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.skus[1].skuId").value(2))
                .andExpect(jsonPath("$.skus[1].skuCode").value("MACBOOK-PRO-16-M3-GRAY"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(productSearchService).findProductById(productId);
    }

    @Test
    @DisplayName("GET /api/catalog/products/{productId} - 존재하지 않는 상품 조회 시 404 반환")
    void getProductDetail_notFound() throws Exception {
        // given
        String productId = "999";

        given(productSearchService.findProductById(productId))
                .willReturn(null);

        // when & then
        mockMvc.perform(get("/api/catalog/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(productSearchService).findProductById(productId);
    }

    @Test
    @DisplayName("GET /api/catalog/products/{productId} - SKU 정보가 없는 상품 조회")
    void getProductDetail_withoutSkus() throws Exception {
        // given
        String productId = "2";
        ProductDocument product = ProductDocument.builder()
                .productId(productId)
                .productName("갤럭시 S24")
                .description("삼성의 최신 플래그십 스마트폰")
                .basePrice(1500000L)
                .salePrice(1400000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/galaxy.jpg")
                .categoryIds(List.of(2L, 20L))
                .searchKeywords(List.of("갤럭시", "스마트폰", "삼성"))
                .skus(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(productSearchService.findProductById(productId))
                .willReturn(product);

        // when & then
        mockMvc.perform(get("/api/catalog/products/{productId}", productId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("갤럭시 S24"))
                .andExpect(jsonPath("$.skus").isEmpty());

        verify(productSearchService).findProductById(productId);
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

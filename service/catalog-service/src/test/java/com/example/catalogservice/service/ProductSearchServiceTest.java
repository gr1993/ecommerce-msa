package com.example.catalogservice.service;

import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchService Mock 테스트")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProductSearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ProductSearchService productSearchService;

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 단일 카테고리 매칭")
    void searchProducts_SingleCategoryMatch() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "노트북", List.of(10L, 20L)),
                createProductDocument("2", "마우스", List.of(10L, 30L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("노트북", "마우스");

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 결과 없음")
    void searchProducts_NoResults() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(99L)
                .page(0)
                .size(10)
                .build();

        SearchHits<ProductDocument> searchHits = createSearchHits(List.of(), 0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 페이지네이션")
    void searchProducts_Pagination() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(100L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품 1", List.of(100L)),
                createProductDocument("2", "상품 2", List.of(100L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 25L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(25);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("카테고리 ID가 null일 때 - 전체 상품 조회 (matchAll)")
    void searchProducts_NullCategoryId_MatchAll() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "노트북", List.of(10L, 20L)),
                createProductDocument("2", "마우스", List.of(30L, 40L)),
                createProductDocument("3", "키보드", List.of(50L, 60L)),
                createProductDocument("4", "모니터", List.of())
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 4L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("노트북", "마우스", "키보드", "모니터");
    }

    @Test
    @DisplayName("상품명으로 검색")
    void searchProducts_ByProductName() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("노트북")
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "삼성 노트북", List.of(10L)),
                createProductDocument("2", "LG 노트북", List.of(10L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allMatch(p -> p.getProductName().contains("노트북"));
    }

    @Test
    @DisplayName("가격 범위로 필터링")
    void searchProducts_ByPriceRange() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(5000L)
                .maxPrice(10000L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L)),
                createProductDocument("2", "상품2", List.of(10L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("상태로 필터링")
    void searchProducts_ByStatus() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("ACTIVE")
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).allMatch(p -> "ACTIVE".equals(p.getStatus()));
    }

    @Test
    @DisplayName("복합 조건 검색 - 상품명 + 카테고리 + 가격")
    void searchProducts_MultipleConditions() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("노트북")
                .categoryId(10L)
                .minPrice(5000L)
                .maxPrice(15000L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "삼성 노트북", List.of(10L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).contains("노트북");
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

    @SuppressWarnings("unchecked")
    private SearchHits<ProductDocument> createSearchHits(List<ProductDocument> products, long totalHits) {
        SearchHits<ProductDocument> searchHits = mock(SearchHits.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        List<SearchHit<ProductDocument>> searchHitList = products.stream()
                .map(product -> new SearchHit<>(
                        null,
                        null,
                        null,
                        1.0f,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        product
                ))
                .toList();

        when(searchHits.getTotalHits()).thenReturn(totalHits);
        when(searchHits.getTotalHitsRelation()).thenReturn(TotalHitsRelation.EQUAL_TO);
        when(searchHits.getSearchHits()).thenReturn(searchHitList);
        when(searchHits.hasSearchHits()).thenReturn(!products.isEmpty());
        when(searchHits.stream()).thenReturn(searchHitList.stream());

        return searchHits;
    }
}

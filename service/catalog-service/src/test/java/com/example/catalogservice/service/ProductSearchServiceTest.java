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

    @Mock
    private CategorySyncService categorySyncService;

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

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
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

        when(categorySyncService.getCategoryIdWithDescendants(99L)).thenReturn(List.of(99L));
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

        when(categorySyncService.getCategoryIdWithDescendants(100L)).thenReturn(List.of(100L));
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

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
        SearchHits<ProductDocument> searchHits = createSearchHits(products, 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getProductName()).contains("노트북");
    }

    @Test
    @DisplayName("상위 카테고리 검색 시 하위 카테고리 상품도 함께 조회")
    void searchProducts_IncludeDescendantCategories() {
        // Given - 전자제품(1) 카테고리와 하위 카테고리(2, 3, 4)가 있는 경우
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(1L)  // 전자제품 (상위 카테고리)
                .page(0)
                .size(10)
                .build();

        // 상위 카테고리 1과 하위 카테고리 2, 3, 4에 속한 상품들
        List<ProductDocument> products = List.of(
                createProductDocument("1", "스마트폰", List.of(2L)),      // 하위 카테고리
                createProductDocument("2", "노트북", List.of(3L)),         // 하위 카테고리
                createProductDocument("3", "태블릿", List.of(4L)),         // 하위 카테고리
                createProductDocument("4", "액세서리", List.of(1L, 2L))   // 상위 + 하위
        );

        // CategorySyncService가 하위 카테고리 ID들을 반환하도록 설정
        when(categorySyncService.getCategoryIdWithDescendants(1L))
                .thenReturn(List.of(1L, 2L, 3L, 4L));

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 4L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("스마트폰", "노트북", "태블릿", "액세서리");

        // CategorySyncService가 호출되었는지 검증
        verify(categorySyncService).getCategoryIdWithDescendants(1L);
    }

    @Test
    @DisplayName("리프 카테고리 검색 시 자기 자신만 조회")
    void searchProducts_LeafCategoryOnly() {
        // Given - 리프 카테고리(하위 카테고리가 없음)
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L)),
                createProductDocument("2", "상품2", List.of(10L))
        );

        // 리프 카테고리는 자기 자신만 반환
        when(categorySyncService.getCategoryIdWithDescendants(10L))
                .thenReturn(List.of(10L));

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(categorySyncService).getCategoryIdWithDescendants(10L);
    }

    @Test
    @DisplayName("중간 계층 카테고리 검색 시 하위만 포함")
    void searchProducts_MiddleLevelCategory() {
        // Given - 스마트폰(2) > 삼성(4), 애플(5)
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(2L)  // 스마트폰 (중간 계층)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "갤럭시 S24", List.of(4L)),    // 삼성
                createProductDocument("2", "아이폰 15", List.of(5L)),     // 애플
                createProductDocument("3", "스마트폰 케이스", List.of(2L, 4L))  // 스마트폰 + 삼성
        );

        // 스마트폰 카테고리와 하위 카테고리 (삼성, 애플)
        when(categorySyncService.getCategoryIdWithDescendants(2L))
                .thenReturn(List.of(2L, 4L, 5L));

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 3L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("갤럭시 S24", "아이폰 15", "스마트폰 케이스");

        verify(categorySyncService).getCategoryIdWithDescendants(2L);
    }

    @Test
    @DisplayName("상품명 자동완성 - 키워드 검색 시 최대 5개의 상품명 반환")
    void autocompleteProductName_withKeyword() {
        // Given
        String keyword = "노트북";
        List<ProductDocument> products = List.of(
                createProductDocument("1", "삼성 노트북", List.of(1L)),
                createProductDocument("2", "LG 노트북", List.of(1L)),
                createProductDocument("3", "애플 노트북 프로", List.of(1L)),
                createProductDocument("4", "HP 노트북", List.of(1L)),
                createProductDocument("5", "레노버 노트북", List.of(1L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 5L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> result = productSearchService.autocompleteProductName(keyword);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result).containsExactlyInAnyOrder(
                "삼성 노트북", "LG 노트북", "애플 노트북 프로", "HP 노트북", "레노버 노트북"
        );

        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("상품명 자동완성 - 빈 키워드 입력 시 빈 리스트 반환")
    void autocompleteProductName_withEmptyKeyword() {
        // Given
        String emptyKeyword = "";

        // When
        List<String> result = productSearchService.autocompleteProductName(emptyKeyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품명 자동완성 - null 키워드 입력 시 빈 리스트 반환")
    void autocompleteProductName_withNullKeyword() {
        // Given
        String nullKeyword = null;

        // When
        List<String> result = productSearchService.autocompleteProductName(nullKeyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품명 자동완성 - 공백 키워드 입력 시 빈 리스트 반환")
    void autocompleteProductName_withBlankKeyword() {
        // Given
        String blankKeyword = "   ";

        // When
        List<String> result = productSearchService.autocompleteProductName(blankKeyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("상품명 자동완성 - 검색 결과가 5개 미만인 경우")
    void autocompleteProductName_lessThanFiveResults() {
        // Given
        String keyword = "아이패드";
        List<ProductDocument> products = List.of(
                createProductDocument("1", "아이패드 프로", List.of(1L)),
                createProductDocument("2", "아이패드 미니", List.of(1L)),
                createProductDocument("3", "아이패드 에어", List.of(1L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 3L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> result = productSearchService.autocompleteProductName(keyword);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(
                "아이패드 프로", "아이패드 미니", "아이패드 에어"
        );
    }

    @Test
    @DisplayName("상품명 자동완성 - 중복된 상품명 제거")
    void autocompleteProductName_removeDuplicates() {
        // Given
        String keyword = "갤럭시";
        List<ProductDocument> products = List.of(
                createProductDocument("1", "갤럭시 S24", List.of(1L)),
                createProductDocument("2", "갤럭시 S24", List.of(1L)),
                createProductDocument("3", "갤럭시 Z Fold", List.of(1L))
        );

        SearchHits<ProductDocument> searchHits = createSearchHits(products, 3L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> result = productSearchService.autocompleteProductName(keyword);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("갤럭시 S24", "갤럭시 Z Fold");
    }

    @Test
    @DisplayName("상품명 자동완성 - 검색 결과 없음")
    void autocompleteProductName_noResults() {
        // Given
        String keyword = "존재하지않는상품";
        SearchHits<ProductDocument> searchHits = createSearchHits(List.of(), 0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        List<String> result = productSearchService.autocompleteProductName(keyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("키워드 검색 시 _score 우선 정렬 후 기본 정렬 적용")
    void searchProducts_WithKeyword_ScoreThenDefaultSort() {
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
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("키워드 검색 시 _score 우선 정렬 후 사용자 지정 정렬 적용")
    void searchProducts_WithKeyword_ScoreThenCustomSort() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("노트북")
                .sort("salePrice,desc")
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
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("키워드 없이 필터만 있을 때 사용자 지정 정렬만 적용")
    void searchProducts_WithoutKeyword_CustomSortOnly() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .sort("salePrice,asc")
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L)),
                createProductDocument("2", "상품2", List.of(10L))
        );

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
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
    @DisplayName("키워드 없이 필터만 있을 때 기본 정렬만 적용")
    void searchProducts_WithoutKeyword_DefaultSortOnly() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L)),
                createProductDocument("2", "상품2", List.of(10L))
        );

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
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
    @DisplayName("정렬 파라미터가 여러 부분으로 구성된 경우 올바르게 파싱")
    void searchProducts_SortParsing() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .sort("salePrice,desc")
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L))
        );

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
        SearchHits<ProductDocument> searchHits = createSearchHits(products, 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
    }

    @Test
    @DisplayName("정렬 방향이 명시되지 않으면 ASC 기본값 적용")
    void searchProducts_SortDirectionDefault() {
        // Given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(10L)
                .sort("salePrice")
                .page(0)
                .size(10)
                .build();

        List<ProductDocument> products = List.of(
                createProductDocument("1", "상품1", List.of(10L))
        );

        when(categorySyncService.getCategoryIdWithDescendants(10L)).thenReturn(List.of(10L));
        SearchHits<ProductDocument> searchHits = createSearchHits(products, 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(ProductDocument.class)))
                .thenReturn(searchHits);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(ProductDocument.class));
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

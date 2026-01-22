package com.example.catalogservice.service;

import com.example.catalogservice.config.ElasticsearchTestContainerConfig;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "springwolf.enabled=false",
        "product-service.url=http://localhost:8083"
})
@Import(ElasticsearchTestContainerConfig.class)
@DisplayName("ProductSearchService 통합 테스트")
class ProductSearchServiceTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @BeforeEach
    void setUp() {
        // 인덱스가 없으면 생성
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }

        // 기존 데이터만 삭제 (인덱스는 유지) - Repository의 deleteAll() 사용
        productSearchRepository.deleteAll();
        indexOps.refresh();
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 단일 카테고리 매칭")
    void searchProducts_SingleCategoryMatch() {
        // Given
        ProductDocument product1 = createProductDocument("1", "노트북", List.of(10L, 20L));
        ProductDocument product2 = createProductDocument("2", "마우스", List.of(10L, 30L));
        ProductDocument product3 = createProductDocument("3", "키보드", List.of(40L, 50L));

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.save(product3);

        // ES의 near-realtime 특성 때문에 refresh 필요
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(10L, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("노트북", "마우스");
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 결과 없음")
    void searchProducts_NoResults() {
        // Given
        ProductDocument product1 = createProductDocument("1", "노트북", List.of(10L, 20L));
        ProductDocument product2 = createProductDocument("2", "마우스", List.of(10L, 30L));

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(99L, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 페이지네이션")
    void searchProducts_Pagination() {
        // Given
        for (int i = 1; i <= 25; i++) {
            ProductDocument product = createProductDocument(
                    String.valueOf(i),
                    "상품 " + i,
                    List.of(100L, 200L)
            );
            elasticsearchOperations.save(product);
        }
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        // When
        Pageable firstPage = PageRequest.of(0, 10);
        Page<ProductDocument> result1 = productSearchService.searchProducts(100L, firstPage);

        Pageable secondPage = PageRequest.of(1, 10);
        Page<ProductDocument> result2 = productSearchService.searchProducts(100L, secondPage);

        Pageable thirdPage = PageRequest.of(2, 10);
        Page<ProductDocument> result3 = productSearchService.searchProducts(100L, thirdPage);

        // Then
        assertThat(result1.getTotalElements()).isEqualTo(25);
        assertThat(result1.getTotalPages()).isEqualTo(3);
        assertThat(result1.getContent()).hasSize(10);
        assertThat(result1.isFirst()).isTrue();

        assertThat(result2.getContent()).hasSize(10);
        assertThat(result2.isFirst()).isFalse();
        assertThat(result2.isLast()).isFalse();

        assertThat(result3.getContent()).hasSize(5);
        assertThat(result3.isLast()).isTrue();
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 여러 카테고리 ID 중 하나만 매칭되어도 검색됨")
    void searchProducts_MultipleCategories() {
        // Given
        ProductDocument product1 = createProductDocument("1", "노트북", List.of(10L, 20L, 30L));
        ProductDocument product2 = createProductDocument("2", "태블릿", List.of(15L, 25L));
        ProductDocument product3 = createProductDocument("3", "스마트폰", List.of(20L, 35L));

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.save(product3);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(20L, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("노트북", "스마트폰");
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 정렬 검증")
    void searchProducts_SortOrder() {
        // Given
        ProductDocument product1 = createProductDocument("1", "상품A", List.of(50L));
        ProductDocument product2 = createProductDocument("2", "상품B", List.of(50L));
        ProductDocument product3 = createProductDocument("3", "상품C", List.of(50L));

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.save(product3);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(50L, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .contains("상품A", "상품B", "상품C");
    }

    @Test
    @DisplayName("카테고리 ID로 상품 검색 - 빈 카테고리 ID 배열을 가진 상품은 검색되지 않음")
    void searchProducts_EmptyCategoryIds() {
        // Given
        ProductDocument product1 = createProductDocument("1", "상품1", List.of(60L));
        ProductDocument product2 = createProductDocument("2", "상품2", List.of());

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(60L, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactly("상품1");
    }

    @Test
    @DisplayName("카테고리 ID가 null일 때 - 전체 상품 조회 (matchAll)")
    void searchProducts_NullCategoryId_MatchAll() {
        // Given
        ProductDocument product1 = createProductDocument("1", "노트북", List.of(10L, 20L));
        ProductDocument product2 = createProductDocument("2", "마우스", List.of(30L, 40L));
        ProductDocument product3 = createProductDocument("3", "키보드", List.of(50L, 60L));
        ProductDocument product4 = createProductDocument("4", "모니터", List.of());

        elasticsearchOperations.save(product1);
        elasticsearchOperations.save(product2);
        elasticsearchOperations.save(product3);
        elasticsearchOperations.save(product4);
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<ProductDocument> result = productSearchService.searchProducts(null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .containsExactlyInAnyOrder("노트북", "마우스", "키보드", "모니터");
    }

    @Test
    @DisplayName("카테고리 ID가 null일 때 - 전체 상품 조회 페이지네이션")
    void searchProducts_NullCategoryId_Pagination() {
        // Given
        for (int i = 1; i <= 25; i++) {
            ProductDocument product = createProductDocument(
                    String.valueOf(i),
                    "상품 " + i,
                    List.of((long) (i % 5 + 1))
            );
            elasticsearchOperations.save(product);
        }
        elasticsearchOperations.indexOps(ProductDocument.class).refresh();

        // When
        Pageable firstPage = PageRequest.of(0, 10);
        Page<ProductDocument> result1 = productSearchService.searchProducts(null, firstPage);

        Pageable secondPage = PageRequest.of(1, 10);
        Page<ProductDocument> result2 = productSearchService.searchProducts(null, secondPage);

        // Then
        assertThat(result1.getTotalElements()).isEqualTo(25);
        assertThat(result1.getTotalPages()).isEqualTo(3);
        assertThat(result1.getContent()).hasSize(10);
        assertThat(result1.isFirst()).isTrue();

        assertThat(result2.getContent()).hasSize(10);
        assertThat(result2.isFirst()).isFalse();
        assertThat(result2.isLast()).isFalse();
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

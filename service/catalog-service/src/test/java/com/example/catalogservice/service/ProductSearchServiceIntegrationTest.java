package com.example.catalogservice.service;

import com.example.catalogservice.config.ElasticsearchTestContainerConfig;
import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
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
class ProductSearchServiceIntegrationTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUp() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
        productSearchRepository.deleteAll();

        // 테스트 데이터 준비
        List<ProductDocument> testProducts = List.of(
                createProduct("1", "맥북 프로 16인치", "애플 M3 맥스 칩 탑재", 3500000L, 3300000L, "ACTIVE", List.of(1L, 10L)),
                createProduct("2", "갤럭시 S24 울트라", "삼성 최신 플래그십 스마트폰", 1500000L, 1400000L, "ACTIVE", List.of(2L, 20L)),
                createProduct("3", "아이패드 프로 12.9", "M2 칩 탑재 태블릿", 1200000L, 1100000L, "ACTIVE", List.of(1L, 11L)),
                createProduct("4", "에어팟 프로 2세대", "노이즈 캔슬링 이어폰", 350000L, 320000L, "ACTIVE", List.of(3L, 30L)),
                createProduct("5", "LG 그램 17", "초경량 노트북", 2000000L, 1800000L, "SOLD_OUT", List.of(1L, 12L)),
                createProduct("6", "갤럭시 탭 S9", "안드로이드 태블릿", 900000L, 850000L, "ACTIVE", List.of(2L, 21L)),
                createProduct("7", "맥북 에어 M2", "가성비 좋은 노트북", 1500000L, 1400000L, "ACTIVE", List.of(1L, 10L))
        );

        productSearchRepository.saveAll(testProducts);

        // Elasticsearch 인덱싱 완료 대기
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("조건 없이 전체 상품 조회")
    void searchProducts_withoutConditions_returnsAllProducts() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getContent()).hasSize(7);
    }

    @Test
    @DisplayName("상품명으로 검색 - 맥북")
    void searchProducts_byProductName_macbook() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("맥북")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .allMatch(name -> name.contains("맥북"));
    }

    @Test
    @DisplayName("상품명으로 검색 - 갤럭시")
    void searchProducts_byProductName_galaxy() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("갤럭시")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ProductDocument::getProductName)
                .allMatch(name -> name.contains("갤럭시"));
    }

    @Test
    @DisplayName("카테고리 ID로 필터링 - 카테고리 1")
    void searchProducts_byCategoryId_category1() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(1L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .allMatch(product -> product.getCategoryIds().contains(1L));
    }

    @Test
    @DisplayName("카테고리 ID로 필터링 - 카테고리 2")
    void searchProducts_byCategoryId_category2() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(2L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allMatch(product -> product.getCategoryIds().contains(2L));
    }

    @Test
    @DisplayName("가격 범위 필터 - 최소 가격만")
    void searchProducts_byMinPrice() {
        // given
        long minPrice = 1400000L;
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(minPrice)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .allMatch(product -> product.getSalePrice() >= minPrice);
    }

    @Test
    @DisplayName("가격 범위 필터 - 최대 가격만")
    void searchProducts_byMaxPrice() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .maxPrice(1000000L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .allMatch(product -> product.getSalePrice() <= 1000000L);
    }

    @Test
    @DisplayName("가격 범위 필터 - 최소/최대 가격 모두")
    void searchProducts_byPriceRange() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(1000000L)
                .maxPrice(2000000L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent())
                .allMatch(product ->
                        product.getSalePrice() >= 1000000L &&
                        product.getSalePrice() <= 2000000L
                );
    }

    @Test
    @DisplayName("상태 필터 - ACTIVE")
    void searchProducts_byStatus_active() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("ACTIVE")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent())
                .allMatch(product -> product.getStatus().equals("ACTIVE"));
    }

    @Test
    @DisplayName("상태 필터 - SOLD_OUT")
    void searchProducts_byStatus_soldOut() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("SOLD_OUT")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .allMatch(product -> product.getStatus().equals("SOLD_OUT"));
    }

    @Test
    @DisplayName("복합 조건 검색 - 상품명 + 카테고리")
    void searchProducts_combinedConditions_nameAndCategory() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("맥북")
                .categoryId(1L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.getContent())
                .allMatch(product ->
                        product.getProductName().contains("맥북") &&
                        product.getCategoryIds().contains(1L)
                );
    }

    @Test
    @DisplayName("복합 조건 검색 - 카테고리 + 가격 범위")
    void searchProducts_combinedConditions_categoryAndPrice() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .categoryId(1L)
                .minPrice(1000000L)
                .maxPrice(2000000L)
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(result.getContent())
                .allMatch(product ->
                        product.getCategoryIds().contains(1L) &&
                        product.getSalePrice() >= 1000000L &&
                        product.getSalePrice() <= 2000000L
                );
    }

    @Test
    @DisplayName("복합 조건 검색 - 상품명 + 가격 + 상태")
    void searchProducts_combinedConditions_namePriceStatus() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("갤럭시")
                .minPrice(1000000L)
                .status("ACTIVE")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent())
                .allMatch(product ->
                        product.getProductName().contains("갤럭시") &&
                        product.getSalePrice() >= 1000000L &&
                        product.getStatus().equals("ACTIVE")
                );
    }

    @Test
    @DisplayName("페이지네이션 테스트 - 첫 페이지")
    void searchProducts_pagination_firstPage() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(0)
                .size(3)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("페이지네이션 테스트 - 두 번째 페이지")
    void searchProducts_pagination_secondPage() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(1)
                .size(3)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getNumber()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("정렬 테스트 - createdAt 내림차순 (기본)")
    void searchProducts_sorting_createdAtDesc() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getContent()).isNotEmpty();
        List<LocalDateTime> createdDates = result.getContent().stream()
                .map(ProductDocument::getCreatedAt)
                .toList();

        for (int i = 0; i < createdDates.size() - 1; i++) {
            assertThat(createdDates.get(i))
                    .isAfterOrEqualTo(createdDates.get(i + 1));
        }
    }

    @Test
    @DisplayName("검색 결과 없음")
    void searchProducts_noResults() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("존재하지않는상품명")
                .page(0)
                .size(10)
                .build();

        // when
        Page<ProductDocument> result = productSearchService.searchProducts(request);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    private ProductDocument createProduct(
            String id,
            String name,
            String description,
            Long basePrice,
            Long salePrice,
            String status,
            List<Long> categoryIds
    ) {
        return ProductDocument.builder()
                .productId(id)
                .productName(name)
                .description(description)
                .basePrice(basePrice)
                .salePrice(salePrice)
                .status(status)
                .primaryImageUrl("https://example.com/image.jpg")
                .categoryIds(categoryIds)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

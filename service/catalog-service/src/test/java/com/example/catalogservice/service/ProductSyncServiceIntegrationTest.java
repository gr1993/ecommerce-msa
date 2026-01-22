package com.example.catalogservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.config.ElasticsearchTestContainerConfig;
import com.example.catalogservice.domain.document.ProductDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "springwolf.enabled=false",
        "product-service.url=http://localhost:8083"
})
@Import(ElasticsearchTestContainerConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSyncServiceIntegrationTest {

    @Autowired
    private ProductSyncService productSyncService;

    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트 전에 기존 인덱스 및 alias 정리
        cleanupTestIndices();
    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트 후 정리
        cleanupTestIndices();
    }

    private void cleanupTestIndices() throws IOException {
        // alias가 가리키는 인덱스들 제거
        Set<String> indices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);
        for (String index : indices) {
            try {
                elasticsearchIndexService.deleteIndex(index);
            } catch (Exception e) {
                // 무시
            }
        }

        // "products" 인덱스 삭제 (Spring Data ES가 자동 생성한 경우)
        // alias와 인덱스 이름 충돌 방지
        try {
            boolean productsIndexExists = elasticsearchClient.indices()
                    .exists(e -> e.index("products")).value();
            if (productsIndexExists) {
                elasticsearchClient.indices().delete(d -> d.index("products"));
            }
        } catch (Exception e) {
            // 인덱스가 없는 경우 무시
        }

        // products_* 패턴의 모든 인덱스 삭제
        elasticsearchIndexService.deleteOldIndices("non-existent-index");
    }

    @Test
    @Order(1)
    @DisplayName("fullSync - 새 인덱스 생성 후 alias가 새 인덱스를 가리킨다")
    void fullSync_CreatesNewIndexAndSwitchesAlias() throws IOException {
        // given
        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(2L, "상품2"),
                        createMockProduct(3L, "상품3")
                ),
                0, 100, 3, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // when
        int totalSynced = productSyncService.fullSync();

        // then
        assertThat(totalSynced).isEqualTo(3);

        // alias가 존재하는지 확인
        assertThat(elasticsearchIndexService.aliasExists(ElasticsearchIndexService.ALIAS_NAME)).isTrue();

        // alias가 가리키는 인덱스 확인
        Set<String> indices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);
        assertThat(indices).hasSize(1);

        String currentIndex = indices.iterator().next();
        assertThat(currentIndex).startsWith("products_");

        // alias를 통해 데이터 조회
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );

        assertThat(searchHits.getTotalHits()).isEqualTo(3);
    }

    @Test
    @Order(2)
    @DisplayName("fullSync - 삭제된 상품이 새 인덱스에 포함되지 않는다 (핵심 테스트)")
    void fullSync_DeletedProductsNotIncludedInNewIndex() throws IOException {
        // given - 첫 번째 fullSync: 상품 3개
        PageResponse<CatalogSyncProductResponse> firstSync = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(2L, "상품2"),
                        createMockProduct(3L, "상품3")
                ),
                0, 100, 3, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(firstSync);
        productSyncService.fullSync();

        // 첫 번째 동기화 후 상태 확인
        SearchHits<ProductDocument> afterFirstSync = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );
        assertThat(afterFirstSync.getTotalHits()).isEqualTo(3);

        // when - 두 번째 fullSync: 상품2가 삭제되어 2개만 반환
        PageResponse<CatalogSyncProductResponse> secondSync = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(3L, "상품3")
                ),
                0, 100, 2, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(secondSync);
        int totalSynced = productSyncService.fullSync();

        // then
        assertThat(totalSynced).isEqualTo(2);

        // alias를 통해 조회하면 2개만 조회됨 (삭제된 상품2는 포함되지 않음)
        SearchHits<ProductDocument> afterSecondSync = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );

        assertThat(afterSecondSync.getTotalHits()).isEqualTo(2);

        // 상품 ID 확인
        List<String> productIds = afterSecondSync.stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getProductId)
                .toList();

        assertThat(productIds).containsExactlyInAnyOrder("1", "3");
        assertThat(productIds).doesNotContain("2");

        // alias가 가리키는 인덱스는 1개만 있어야 함 (이전 인덱스 삭제됨)
        Set<String> indices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);
        assertThat(indices).hasSize(1);
    }

    @Test
    @Order(3)
    @DisplayName("fullSync - 여러 페이지 데이터를 새 인덱스에 동기화")
    void fullSync_MultiplePages() throws IOException {
        // given
        PageResponse<CatalogSyncProductResponse> page0 = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(2L, "상품2")
                ),
                0, 2, 5, 3, true, false
        );

        PageResponse<CatalogSyncProductResponse> page1 = createPageResponse(
                List.of(
                        createMockProduct(3L, "상품3"),
                        createMockProduct(4L, "상품4")
                ),
                1, 2, 5, 3, false, false
        );

        PageResponse<CatalogSyncProductResponse> page2 = createPageResponse(
                List.of(
                        createMockProduct(5L, "상품5")
                ),
                2, 2, 5, 3, false, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page0);
        when(productServiceClient.getProductsForSync(1, 100)).thenReturn(page1);
        when(productServiceClient.getProductsForSync(2, 100)).thenReturn(page2);

        // when
        int totalSynced = productSyncService.fullSync();

        // then
        assertThat(totalSynced).isEqualTo(5);

        // alias를 통해 5개 상품 조회 확인
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );

        assertThat(searchHits.getTotalHits()).isEqualTo(5);
    }

    @Test
    @Order(4)
    @DisplayName("fullSync - 이전 인덱스가 삭제된다")
    void fullSync_DeletesOldIndices() throws IOException {
        // given - 첫 번째 동기화
        PageResponse<CatalogSyncProductResponse> firstSync = createPageResponse(
                List.of(createMockProduct(1L, "상품1")),
                0, 100, 1, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(firstSync);
        productSyncService.fullSync();

        Set<String> firstIndices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);
        assertThat(firstIndices).hasSize(1);
        String firstIndexName = firstIndices.iterator().next();

        // when - 두 번째 동기화
        PageResponse<CatalogSyncProductResponse> secondSync = createPageResponse(
                List.of(createMockProduct(2L, "상품2")),
                0, 100, 1, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(secondSync);
        productSyncService.fullSync();

        // then - 첫 번째 인덱스는 삭제되어야 함
        assertThat(elasticsearchIndexService.indexExists(firstIndexName)).isFalse();

        // 두 번째 인덱스만 존재
        Set<String> secondIndices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);
        assertThat(secondIndices).hasSize(1);

        String secondIndexName = secondIndices.iterator().next();
        assertThat(secondIndexName).isNotEqualTo(firstIndexName);
        assertThat(elasticsearchIndexService.indexExists(secondIndexName)).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("fullSync - 실패 시 새 인덱스가 롤백된다")
    void fullSync_RollbackOnFailure() throws IOException {
        // given - ProductServiceClient가 예외를 던지도록 설정
        when(productServiceClient.getProductsForSync(anyInt(), anyInt()))
                .thenThrow(new RuntimeException("External API failure"));

        // when & then
        assertThatThrownBy(() -> productSyncService.fullSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Full sync failed");

        // 실패 후 인덱스가 생성되지 않았는지 확인
        // alias가 존재하지 않아야 함
        assertThat(elasticsearchIndexService.aliasExists(ElasticsearchIndexService.ALIAS_NAME)).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("fullSync - 상품 데이터 매핑 검증")
    void fullSync_ProductDataMapping() throws IOException {
        // given
        LocalDateTime now = LocalDateTime.now();
        CatalogSyncProductResponse product = createMockProduct(100L, "테스트 상품");
        ReflectionTestUtils.setField(product, "description", "테스트 설명입니다");
        ReflectionTestUtils.setField(product, "basePrice", 50000L);
        ReflectionTestUtils.setField(product, "salePrice", 45000L);
        ReflectionTestUtils.setField(product, "status", "ON_SALE");
        ReflectionTestUtils.setField(product, "primaryImageUrl", "https://example.com/test.jpg");
        ReflectionTestUtils.setField(product, "categoryIds", List.of(10L, 20L, 30L));
        ReflectionTestUtils.setField(product, "createdAt", now);
        ReflectionTestUtils.setField(product, "updatedAt", now);

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // when
        productSyncService.fullSync();

        // then
        ProductDocument saved = elasticsearchOperations.get("100", ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(saved).isNotNull();
        assertThat(saved.getProductId()).isEqualTo("100");
        assertThat(saved.getProductName()).isEqualTo("테스트 상품");
        assertThat(saved.getDescription()).isEqualTo("테스트 설명입니다");
        assertThat(saved.getBasePrice()).isEqualTo(50000L);
        assertThat(saved.getSalePrice()).isEqualTo(45000L);
        assertThat(saved.getStatus()).isEqualTo("ON_SALE");
        assertThat(saved.getPrimaryImageUrl()).isEqualTo("https://example.com/test.jpg");
        assertThat(saved.getCategoryIds()).containsExactly(10L, 20L, 30L);
    }

    @Test
    @Order(8)
    @DisplayName("fullSync - 카테고리 ID가 없는 상품도 동기화된다")
    void fullSync_ProductWithoutCategoryIds() throws IOException {
        // given
        CatalogSyncProductResponse product = createMockProduct(200L, "카테고리 없는 상품");
        ReflectionTestUtils.setField(product, "categoryIds", null);

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // when
        int totalSynced = productSyncService.fullSync();

        // then
        assertThat(totalSynced).isEqualTo(1);

        ProductDocument saved = elasticsearchOperations.get("200", ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(saved).isNotNull();
        assertThat(saved.getCategoryIds()).isNull();
    }

    @Test
    @Order(9)
    @DisplayName("fullSync - 연속 동기화 시나리오 (실제 운영 환경 시뮬레이션)")
    void fullSync_ConsecutiveSyncScenario() throws IOException {
        // given - 첫 번째 동기화: 상품 5개
        PageResponse<CatalogSyncProductResponse> sync1 = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(2L, "상품2"),
                        createMockProduct(3L, "상품3"),
                        createMockProduct(4L, "상품4"),
                        createMockProduct(5L, "상품5")
                ),
                0, 100, 5, 1, true, true
        );
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(sync1);
        productSyncService.fullSync();

        // when - 두 번째 동기화: 상품 2, 4 삭제, 상품 6, 7 추가
        PageResponse<CatalogSyncProductResponse> sync2 = createPageResponse(
                List.of(
                        createMockProduct(1L, "상품1"),
                        createMockProduct(3L, "상품3"),
                        createMockProduct(5L, "상품5"),
                        createMockProduct(6L, "상품6"),
                        createMockProduct(7L, "상품7")
                ),
                0, 100, 5, 1, true, true
        );
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(sync2);
        productSyncService.fullSync();

        // then
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );

        assertThat(searchHits.getTotalHits()).isEqualTo(5);

        List<String> productIds = searchHits.stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getProductId)
                .toList();

        assertThat(productIds).containsExactlyInAnyOrder("1", "3", "5", "6", "7");
        assertThat(productIds).doesNotContain("2", "4");

        // when - 세 번째 동기화: 모든 상품 삭제, 새 상품 10, 11, 12 추가
        PageResponse<CatalogSyncProductResponse> sync3 = createPageResponse(
                List.of(
                        createMockProduct(10L, "상품10"),
                        createMockProduct(11L, "상품11"),
                        createMockProduct(12L, "상품12")
                ),
                0, 100, 3, 1, true, true
        );
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(sync3);
        productSyncService.fullSync();

        // then
        SearchHits<ProductDocument> finalSearchHits = elasticsearchOperations.search(
                Query.findAll(),
                ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME)
        );

        assertThat(finalSearchHits.getTotalHits()).isEqualTo(3);

        List<String> finalProductIds = finalSearchHits.stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::getProductId)
                .toList();

        assertThat(finalProductIds).containsExactlyInAnyOrder("10", "11", "12");
    }

    private PageResponse<CatalogSyncProductResponse> createPageResponse(
            List<CatalogSyncProductResponse> content,
            int page, int size, long totalElements, int totalPages,
            boolean first, boolean last) {

        try {
            java.lang.reflect.Constructor<PageResponse> constructor =
                    (java.lang.reflect.Constructor<PageResponse>) PageResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PageResponse<CatalogSyncProductResponse> pageResponse = constructor.newInstance();

            ReflectionTestUtils.setField(pageResponse, "content", content);
            ReflectionTestUtils.setField(pageResponse, "page", page);
            ReflectionTestUtils.setField(pageResponse, "size", size);
            ReflectionTestUtils.setField(pageResponse, "totalElements", totalElements);
            ReflectionTestUtils.setField(pageResponse, "totalPages", totalPages);
            ReflectionTestUtils.setField(pageResponse, "first", first);
            ReflectionTestUtils.setField(pageResponse, "last", last);
            return pageResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PageResponse", e);
        }
    }

    private CatalogSyncProductResponse createMockProduct(Long id, String name) {
        try {
            java.lang.reflect.Constructor<CatalogSyncProductResponse> constructor =
                    CatalogSyncProductResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            CatalogSyncProductResponse product = constructor.newInstance();

            ReflectionTestUtils.setField(product, "productId", id);
            ReflectionTestUtils.setField(product, "productName", name);
            ReflectionTestUtils.setField(product, "description", "Description for " + name);
            ReflectionTestUtils.setField(product, "basePrice", 10000L);
            ReflectionTestUtils.setField(product, "salePrice", 8000L);
            ReflectionTestUtils.setField(product, "status", "ACTIVE");
            ReflectionTestUtils.setField(product, "primaryImageUrl", "https://example.com/image.jpg");
            ReflectionTestUtils.setField(product, "categoryIds", List.of(1L, 2L));
            ReflectionTestUtils.setField(product, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(product, "updatedAt", LocalDateTime.now());
            return product;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock product", e);
        }
    }
}

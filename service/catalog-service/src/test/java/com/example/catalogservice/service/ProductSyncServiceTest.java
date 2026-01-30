package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSyncService 단위 테스트")
class ProductSyncServiceTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ElasticsearchIndexService elasticsearchIndexService;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private IndexOperations indexOperations;

    private ProductSyncService productSyncService;

    @Captor
    private ArgumentCaptor<List<IndexQuery>> indexQueriesCaptor;

    @Captor
    private ArgumentCaptor<IndexCoordinates> indexCoordinatesCaptor;

    private static final String TEST_INDEX_NAME = "products_20240101_120000";

    @BeforeEach
    void setUp() {
        productSyncService = new ProductSyncService(
                productServiceClient,
                elasticsearchOperations,
                elasticsearchIndexService,
                productSearchRepository
        );

        // indexOps().refresh() 호출을 위한 mock 설정
        // lenient: 일부 테스트에서는 이 stubbing이 사용되지 않을 수 있음 (예: 인덱스 생성 실패 시)
        lenient().when(elasticsearchOperations.indexOps(any(IndexCoordinates.class))).thenReturn(indexOperations);
    }

    @Test
    @DisplayName("전체 동기화 - 단일 페이지 성공")
    void fullSync_SinglePage_Success() throws IOException {
        // Given
        PageResponse<CatalogSyncProductResponse> firstPage = createPageResponse(
                List.of(
                        createMockProduct(1L, "Product 1"),
                        createMockProduct(2L, "Product 2")
                ),
                0, 100, 2, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(firstPage);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(2);
        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(elasticsearchOperations, times(1)).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService).deleteOldIndices(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - 다중 페이지 성공")
    void fullSync_MultiplePages_Success() throws IOException {
        // Given
        PageResponse<CatalogSyncProductResponse> page0 = createPageResponse(
                List.of(
                        createMockProduct(1L, "Product 1"),
                        createMockProduct(2L, "Product 2")
                ),
                0, 2, 5, 3, true, false
        );

        PageResponse<CatalogSyncProductResponse> page1 = createPageResponse(
                List.of(
                        createMockProduct(3L, "Product 3"),
                        createMockProduct(4L, "Product 4")
                ),
                1, 2, 5, 3, false, false
        );

        PageResponse<CatalogSyncProductResponse> page2 = createPageResponse(
                List.of(
                        createMockProduct(5L, "Product 5")
                ),
                2, 2, 5, 3, false, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page0);
        when(productServiceClient.getProductsForSync(1, 100)).thenReturn(page1);
        when(productServiceClient.getProductsForSync(2, 100)).thenReturn(page2);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(5);
        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(productServiceClient, times(1)).getProductsForSync(1, 100);
        verify(productServiceClient, times(1)).getProductsForSync(2, 100);
        verify(elasticsearchOperations, times(3)).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService).deleteOldIndices(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - 빈 응답 처리")
    void fullSync_EmptyResponse() throws IOException {
        // Given
        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(null);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(elasticsearchOperations, never()).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService).deleteOldIndices(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - 빈 컨텐츠 처리")
    void fullSync_EmptyContent() throws IOException {
        // Given
        PageResponse<CatalogSyncProductResponse> emptyPage = createPageResponse(
                List.of(),
                0, 100, 0, 0, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(emptyPage);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(elasticsearchOperations, never()).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService).deleteOldIndices(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - ProductDocument 매핑 및 IndexQuery 생성 검증")
    void fullSync_DocumentMapping_Verified() throws IOException {
        // Given
        LocalDateTime now = LocalDateTime.now();
        CatalogSyncProductResponse product = createMockProduct(100L, "Test Product");
        ReflectionTestUtils.setField(product, "description", "Test Description");
        ReflectionTestUtils.setField(product, "basePrice", 20000L);
        ReflectionTestUtils.setField(product, "salePrice", 15000L);
        ReflectionTestUtils.setField(product, "status", "ACTIVE");
        ReflectionTestUtils.setField(product, "primaryImageUrl", "https://test.com/image.jpg");
        ReflectionTestUtils.setField(product, "categoryIds", List.of(10L, 20L, 30L));
        ReflectionTestUtils.setField(product, "createdAt", now);
        ReflectionTestUtils.setField(product, "updatedAt", now);

        // SKU 정보 추가
        List<CatalogSyncProductResponse.SkuSnapshot> skus = List.of(
                new CatalogSyncProductResponse.SkuSnapshot(1L, "SKU-001", java.math.BigDecimal.valueOf(15000), 100, "ACTIVE"),
                new CatalogSyncProductResponse.SkuSnapshot(2L, "SKU-002", java.math.BigDecimal.valueOf(18000), 50, "ACTIVE")
        );
        ReflectionTestUtils.setField(product, "skus", skus);

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // When
        productSyncService.fullSync();

        // Then
        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), indexCoordinatesCaptor.capture());

        List<IndexQuery> indexQueries = indexQueriesCaptor.getValue();
        assertThat(indexQueries).hasSize(1);

        IndexQuery indexQuery = indexQueries.get(0);
        assertThat(indexQuery.getId()).isEqualTo("100");

        ProductDocument doc = (ProductDocument) indexQuery.getObject();
        assertThat(doc.getProductId()).isEqualTo("100");
        assertThat(doc.getProductName()).isEqualTo("Test Product");
        assertThat(doc.getDescription()).isEqualTo("Test Description");
        assertThat(doc.getBasePrice()).isEqualTo(20000L);
        assertThat(doc.getSalePrice()).isEqualTo(15000L);
        assertThat(doc.getStatus()).isEqualTo("ACTIVE");
        assertThat(doc.getPrimaryImageUrl()).isEqualTo("https://test.com/image.jpg");
        assertThat(doc.getCategoryIds()).containsExactly(10L, 20L, 30L);
        assertThat(doc.getCreatedAt()).isEqualTo(now);
        assertThat(doc.getUpdatedAt()).isEqualTo(now);

        // SKU 매핑 검증
        assertThat(doc.getSkus()).hasSize(2);
        assertThat(doc.getSkus().get(0).getSkuId()).isEqualTo(1L);
        assertThat(doc.getSkus().get(0).getSkuCode()).isEqualTo("SKU-001");
        assertThat(doc.getSkus().get(0).getPrice()).isEqualTo(15000L);
        assertThat(doc.getSkus().get(0).getStockQty()).isEqualTo(100);
        assertThat(doc.getSkus().get(0).getStatus()).isEqualTo("ACTIVE");

        assertThat(doc.getSkus().get(1).getSkuId()).isEqualTo(2L);
        assertThat(doc.getSkus().get(1).getSkuCode()).isEqualTo("SKU-002");
        assertThat(doc.getSkus().get(1).getPrice()).isEqualTo(18000L);
        assertThat(doc.getSkus().get(1).getStockQty()).isEqualTo(50);
        assertThat(doc.getSkus().get(1).getStatus()).isEqualTo("ACTIVE");

        // IndexCoordinates가 새 인덱스를 가리키는지 확인
        IndexCoordinates indexCoordinates = indexCoordinatesCaptor.getValue();
        assertThat(indexCoordinates.getIndexName()).isEqualTo(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - 대용량 데이터 처리")
    void fullSync_LargeDataSet() throws IOException {
        // Given
        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);

        // 10페이지, 각 100개씩 = 총 1000개
        for (int i = 0; i < 10; i++) {
            List<CatalogSyncProductResponse> products = createMultipleProducts(i * 100, 100);
            PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                    products,
                    i, 100, 1000, 10, i == 0, i == 9
            );
            when(productServiceClient.getProductsForSync(i, 100)).thenReturn(page);
        }

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(1000);
        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, times(10)).getProductsForSync(anyInt(), eq(100));
        verify(elasticsearchOperations, times(10)).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService).deleteOldIndices(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 실패 시 새 인덱스 삭제 (롤백)")
    void fullSync_Failure_RollbackNewIndex() throws IOException {
        // Given
        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100))
                .thenThrow(new RuntimeException("Full sync failed"));

        // When & Then
        assertThatThrownBy(() -> productSyncService.fullSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Full sync failed");

        verify(elasticsearchIndexService).createNewIndex();
        verify(elasticsearchIndexService, never()).switchAlias(anyString());
        verify(elasticsearchIndexService, never()).deleteOldIndices(anyString());
        verify(elasticsearchIndexService).deleteIndex(TEST_INDEX_NAME);
    }

    @Test
    @DisplayName("전체 동기화 - 인덱스 생성 실패 시 예외 발생")
    void fullSync_IndexCreationFailed() throws IOException {
        // Given
        when(elasticsearchIndexService.createNewIndex())
                .thenThrow(new IOException("Failed to create index"));

        // When & Then
        assertThatThrownBy(() -> productSyncService.fullSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Full sync failed");

        verify(elasticsearchIndexService).createNewIndex();
        verify(productServiceClient, never()).getProductsForSync(anyInt(), anyInt());
    }

    @Test
    @DisplayName("전체 동기화 - alias 전환 실패 시 새 인덱스 삭제")
    void fullSync_AliasSwitchFailed_RollbackNewIndex() throws IOException {
        // Given
        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(createMockProduct(1L, "Product 1")),
                0, 100, 1, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);
        doThrow(new IOException("Failed to switch alias"))
                .when(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);

        // When & Then
        assertThatThrownBy(() -> productSyncService.fullSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Full sync failed");

        verify(elasticsearchIndexService).createNewIndex();
        verify(elasticsearchOperations).bulkIndex(anyList(), any(IndexCoordinates.class));
        verify(elasticsearchIndexService).switchAlias(TEST_INDEX_NAME);
        verify(elasticsearchIndexService, never()).deleteOldIndices(anyString());
        verify(elasticsearchIndexService).deleteIndex(TEST_INDEX_NAME);
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

    private List<CatalogSyncProductResponse> createMultipleProducts(int startId, int count) {
        return java.util.stream.IntStream.range(startId, startId + count)
                .mapToObj(i -> createMockProduct((long) i, "Product " + i))
                .toList();
    }

    @Test
    @DisplayName("상품 생성 이벤트 처리 - 새 상품 인덱싱")
    void indexProduct_Success() {
        // Given
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(100L)
                .productCode("PROD-100")
                .productName("New Product")
                .description("New Product Description")
                .basePrice(java.math.BigDecimal.valueOf(20000))
                .salePrice(java.math.BigDecimal.valueOf(15000))
                .status("ACTIVE")
                .isDisplayed(true)
                .primaryImageUrl("https://example.com/new.jpg")
                .categoryIds(List.of(10L, 20L))
                .skus(List.of(
                        ProductCreatedEvent.SkuSnapshot.builder()
                                .skuId(1L)
                                .skuCode("SKU-001")
                                .price(java.math.BigDecimal.valueOf(15000))
                                .stockQty(100)
                                .status("ACTIVE")
                                .build()
                ))
                .createdAt(createdAt)
                .build();

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.indexProduct(event);

        // Then
        verify(productSearchRepository).save(documentCaptor.capture());

        ProductDocument savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getProductId()).isEqualTo("100");
        assertThat(savedDoc.getProductName()).isEqualTo("New Product");
        assertThat(savedDoc.getDescription()).isEqualTo("New Product Description");
        assertThat(savedDoc.getBasePrice()).isEqualTo(20000L);
        assertThat(savedDoc.getSalePrice()).isEqualTo(15000L);
        assertThat(savedDoc.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedDoc.getPrimaryImageUrl()).isEqualTo("https://example.com/new.jpg");
        assertThat(savedDoc.getCategoryIds()).containsExactly(10L, 20L);
        assertThat(savedDoc.getCreatedAt()).isEqualTo(createdAt);
        assertThat(savedDoc.getUpdatedAt()).isNull();

        // SKU 매핑 검증
        assertThat(savedDoc.getSkus()).hasSize(1);
        assertThat(savedDoc.getSkus().get(0).getSkuId()).isEqualTo(1L);
        assertThat(savedDoc.getSkus().get(0).getSkuCode()).isEqualTo("SKU-001");
        assertThat(savedDoc.getSkus().get(0).getPrice()).isEqualTo(15000L);
        assertThat(savedDoc.getSkus().get(0).getStockQty()).isEqualTo(100);
        assertThat(savedDoc.getSkus().get(0).getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("상품 수정 이벤트 처리 - 기존 문서의 createdAt 보존")
    void updateProduct_PreservesCreatedAt() {
        // Given
        LocalDateTime originalCreatedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 14, 30);

        // 기존 ES 문서
        ProductDocument existingDocument = ProductDocument.builder()
                .productId("100")
                .productName("Old Product Name")
                .description("Old Description")
                .basePrice(10000L)
                .salePrice(8000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/old.jpg")
                .categoryIds(List.of(1L, 2L))
                .skus(List.of(
                        ProductDocument.SkuInfo.builder()
                                .skuId(1L)
                                .skuCode("SKU-OLD")
                                .price(8000L)
                                .stockQty(50)
                                .status("ACTIVE")
                                .build()
                ))
                .createdAt(originalCreatedAt)
                .updatedAt(LocalDateTime.of(2024, 1, 10, 9, 0))
                .build();

        // 업데이트 이벤트 (createdAt 포함하지 않음)
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(100L)
                .productCode("PROD-100")
                .productName("Updated Product Name")
                .description("Updated Description")
                .basePrice(java.math.BigDecimal.valueOf(25000))
                .salePrice(java.math.BigDecimal.valueOf(20000))
                .status("ACTIVE")
                .isDisplayed(true)
                .primaryImageUrl("https://example.com/updated.jpg")
                .categoryIds(List.of(10L, 20L, 30L))
                .skus(List.of(
                        ProductUpdatedEvent.SkuSnapshot.builder()
                                .skuId(1L)
                                .skuCode("SKU-NEW")
                                .price(java.math.BigDecimal.valueOf(20000))
                                .stockQty(100)
                                .status("ACTIVE")
                                .build(),
                        ProductUpdatedEvent.SkuSnapshot.builder()
                                .skuId(2L)
                                .skuCode("SKU-NEW-2")
                                .price(java.math.BigDecimal.valueOf(22000))
                                .stockQty(75)
                                .status("ACTIVE")
                                .build()
                ))
                .updatedAt(updatedAt)
                .build();

        when(productSearchRepository.findById("100"))
                .thenReturn(java.util.Optional.of(existingDocument));

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.updateProduct(event);

        // Then
        verify(productSearchRepository).findById("100");
        verify(productSearchRepository).save(documentCaptor.capture());

        ProductDocument savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getProductId()).isEqualTo("100");
        assertThat(savedDoc.getProductName()).isEqualTo("Updated Product Name");
        assertThat(savedDoc.getDescription()).isEqualTo("Updated Description");
        assertThat(savedDoc.getBasePrice()).isEqualTo(25000L);
        assertThat(savedDoc.getSalePrice()).isEqualTo(20000L);
        assertThat(savedDoc.getStatus()).isEqualTo("ACTIVE");
        assertThat(savedDoc.getPrimaryImageUrl()).isEqualTo("https://example.com/updated.jpg");
        assertThat(savedDoc.getCategoryIds()).containsExactly(10L, 20L, 30L);

        // 핵심 검증: createdAt이 기존 문서의 값으로 보존되어야 함
        assertThat(savedDoc.getCreatedAt())
                .as("createdAt should be preserved from existing document")
                .isEqualTo(originalCreatedAt);

        assertThat(savedDoc.getUpdatedAt()).isEqualTo(updatedAt);

        // SKU 업데이트 검증
        assertThat(savedDoc.getSkus()).hasSize(2);
        assertThat(savedDoc.getSkus().get(0).getSkuId()).isEqualTo(1L);
        assertThat(savedDoc.getSkus().get(0).getSkuCode()).isEqualTo("SKU-NEW");
        assertThat(savedDoc.getSkus().get(0).getPrice()).isEqualTo(20000L);
        assertThat(savedDoc.getSkus().get(0).getStockQty()).isEqualTo(100);

        assertThat(savedDoc.getSkus().get(1).getSkuId()).isEqualTo(2L);
        assertThat(savedDoc.getSkus().get(1).getSkuCode()).isEqualTo("SKU-NEW-2");
        assertThat(savedDoc.getSkus().get(1).getPrice()).isEqualTo(22000L);
        assertThat(savedDoc.getSkus().get(1).getStockQty()).isEqualTo(75);
    }

    @Test
    @DisplayName("상품 수정 이벤트 처리 - 기존 문서가 없을 때 createdAt null")
    void updateProduct_NoExistingDocument_CreatedAtIsNull() {
        // Given
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 14, 30);

        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(999L)
                .productCode("PROD-999")
                .productName("New Product via Update Event")
                .description("Description")
                .basePrice(java.math.BigDecimal.valueOf(30000))
                .salePrice(java.math.BigDecimal.valueOf(25000))
                .status("ACTIVE")
                .isDisplayed(true)
                .primaryImageUrl("https://example.com/new-via-update.jpg")
                .categoryIds(List.of(5L, 6L))
                .updatedAt(updatedAt)
                .build();

        when(productSearchRepository.findById("999"))
                .thenReturn(java.util.Optional.empty());

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.updateProduct(event);

        // Then
        verify(productSearchRepository).findById("999");
        verify(productSearchRepository).save(documentCaptor.capture());

        ProductDocument savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getProductId()).isEqualTo("999");
        assertThat(savedDoc.getProductName()).isEqualTo("New Product via Update Event");

        // 기존 문서가 없으므로 createdAt은 null이어야 함
        assertThat(savedDoc.getCreatedAt())
                .as("createdAt should be null when no existing document found")
                .isNull();

        assertThat(savedDoc.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("상품 수정 이벤트 처리 - BigDecimal null 값 처리")
    void updateProduct_WithNullPrices() {
        // Given
        LocalDateTime originalCreatedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 15, 14, 30);

        ProductDocument existingDocument = ProductDocument.builder()
                .productId("100")
                .productName("Product")
                .createdAt(originalCreatedAt)
                .build();

        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(100L)
                .productName("Updated Product")
                .basePrice(null)  // null price
                .salePrice(null)  // null price
                .updatedAt(updatedAt)
                .build();

        when(productSearchRepository.findById("100"))
                .thenReturn(java.util.Optional.of(existingDocument));

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.updateProduct(event);

        // Then
        verify(productSearchRepository).save(documentCaptor.capture());

        ProductDocument savedDoc = documentCaptor.getValue();
        assertThat(savedDoc.getBasePrice()).isNull();
        assertThat(savedDoc.getSalePrice()).isNull();
        assertThat(savedDoc.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("전체 동기화 - SKU가 null인 경우 빈 리스트 처리")
    void fullSync_WithNullSkus_EmptyList() throws IOException {
        // Given
        CatalogSyncProductResponse product = createMockProduct(100L, "Product without SKUs");
        ReflectionTestUtils.setField(product, "skus", null);

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // When
        productSyncService.fullSync();

        // Then
        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), any(IndexCoordinates.class));

        List<IndexQuery> indexQueries = indexQueriesCaptor.getValue();
        ProductDocument doc = (ProductDocument) indexQueries.get(0).getObject();

        assertThat(doc.getSkus()).isEmpty();
    }

    @Test
    @DisplayName("전체 동기화 - SKU가 빈 리스트인 경우 빈 리스트 처리")
    void fullSync_WithEmptySkus_EmptyList() throws IOException {
        // Given
        CatalogSyncProductResponse product = createMockProduct(100L, "Product with empty SKUs");
        ReflectionTestUtils.setField(product, "skus", List.of());

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // When
        productSyncService.fullSync();

        // Then
        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), any(IndexCoordinates.class));

        List<IndexQuery> indexQueries = indexQueriesCaptor.getValue();
        ProductDocument doc = (ProductDocument) indexQueries.get(0).getObject();

        assertThat(doc.getSkus()).isEmpty();
    }

    @Test
    @DisplayName("상품 생성 이벤트 - SKU가 null인 경우 빈 리스트 처리")
    void indexProduct_WithNullSkus_EmptyList() {
        // Given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(100L)
                .productName("Product without SKUs")
                .skus(null)
                .createdAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.indexProduct(event);

        // Then
        verify(productSearchRepository).save(documentCaptor.capture());
        ProductDocument savedDoc = documentCaptor.getValue();

        assertThat(savedDoc.getSkus()).isEmpty();
    }

    @Test
    @DisplayName("상품 수정 이벤트 - SKU가 null인 경우 빈 리스트 처리")
    void updateProduct_WithNullSkus_EmptyList() {
        // Given
        ProductDocument existingDocument = ProductDocument.builder()
                .productId("100")
                .productName("Product")
                .createdAt(LocalDateTime.now())
                .build();

        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(100L)
                .productName("Updated Product")
                .skus(null)
                .updatedAt(LocalDateTime.now())
                .build();

        when(productSearchRepository.findById("100"))
                .thenReturn(java.util.Optional.of(existingDocument));

        ArgumentCaptor<ProductDocument> documentCaptor = ArgumentCaptor.forClass(ProductDocument.class);

        // When
        productSyncService.updateProduct(event);

        // Then
        verify(productSearchRepository).save(documentCaptor.capture());
        ProductDocument savedDoc = documentCaptor.getValue();

        assertThat(savedDoc.getSkus()).isEmpty();
    }

    @Test
    @DisplayName("전체 동기화 - SKU BigDecimal 가격이 null인 경우 null로 변환")
    void fullSync_WithNullSkuPrice() throws IOException {
        // Given
        CatalogSyncProductResponse product = createMockProduct(100L, "Product");
        List<CatalogSyncProductResponse.SkuSnapshot> skus = List.of(
                new CatalogSyncProductResponse.SkuSnapshot(1L, "SKU-001", null, 100, "ACTIVE")
        );
        ReflectionTestUtils.setField(product, "skus", skus);

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(elasticsearchIndexService.createNewIndex()).thenReturn(TEST_INDEX_NAME);
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // When
        productSyncService.fullSync();

        // Then
        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), any(IndexCoordinates.class));

        List<IndexQuery> indexQueries = indexQueriesCaptor.getValue();
        ProductDocument doc = (ProductDocument) indexQueries.get(0).getObject();

        assertThat(doc.getSkus()).hasSize(1);
        assertThat(doc.getSkus().get(0).getPrice()).isNull();
    }
}

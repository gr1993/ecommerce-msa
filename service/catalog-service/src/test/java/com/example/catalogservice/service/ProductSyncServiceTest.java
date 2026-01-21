package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSyncService 단위 테스트")
class ProductSyncServiceTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private ProductSearchRepository productSearchRepository;

    @InjectMocks
    private ProductSyncService productSyncService;

    @Captor
    private ArgumentCaptor<List<ProductDocument>> documentsCaptor;

    @Test
    @DisplayName("전체 동기화 - 단일 페이지 성공")
    void fullSync_SinglePage_Success() {
        // Given
        PageResponse<CatalogSyncProductResponse> firstPage = createPageResponse(
                List.of(
                        createMockProduct(1L, "Product 1"),
                        createMockProduct(2L, "Product 2")
                ),
                0, 100, 2, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(firstPage);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(2);
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(productSearchRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("전체 동기화 - 다중 페이지 성공")
    void fullSync_MultiplePages_Success() {
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

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page0);
        when(productServiceClient.getProductsForSync(1, 100)).thenReturn(page1);
        when(productServiceClient.getProductsForSync(2, 100)).thenReturn(page2);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(5);
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(productServiceClient, times(1)).getProductsForSync(1, 100);
        verify(productServiceClient, times(1)).getProductsForSync(2, 100);
        verify(productSearchRepository, times(3)).saveAll(anyList());
    }

    @Test
    @DisplayName("전체 동기화 - 빈 응답 처리")
    void fullSync_EmptyResponse() {
        // Given
        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(null);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(productSearchRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("전체 동기화 - 빈 컨텐츠 처리")
    void fullSync_EmptyContent() {
        // Given
        PageResponse<CatalogSyncProductResponse> emptyPage = createPageResponse(
                List.of(),
                0, 100, 0, 0, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(emptyPage);

        // When
        int result = productSyncService.fullSync();

        // Then
        assertThat(result).isEqualTo(0);
        verify(productServiceClient, times(1)).getProductsForSync(0, 100);
        verify(productSearchRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("전체 동기화 - ProductDocument 매핑 검증")
    void fullSync_DocumentMapping_Verified() {
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

        PageResponse<CatalogSyncProductResponse> page = createPageResponse(
                List.of(product),
                0, 100, 1, 1, true, true
        );

        when(productServiceClient.getProductsForSync(0, 100)).thenReturn(page);

        // When
        productSyncService.fullSync();

        // Then
        verify(productSearchRepository).saveAll(documentsCaptor.capture());
        List<ProductDocument> savedDocuments = documentsCaptor.getValue();

        assertThat(savedDocuments).hasSize(1);
        ProductDocument doc = savedDocuments.get(0);
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
    }

    @Test
    @DisplayName("전체 동기화 - 대용량 데이터 처리")
    void fullSync_LargeDataSet() {
        // Given
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
        verify(productServiceClient, times(10)).getProductsForSync(anyInt(), eq(100));
        verify(productSearchRepository, times(10)).saveAll(anyList());
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
}

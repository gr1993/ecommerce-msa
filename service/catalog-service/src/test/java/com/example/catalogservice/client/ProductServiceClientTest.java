package com.example.catalogservice.client;

import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceClient 단위 테스트")
class ProductServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProductServiceClient productServiceClient;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productServiceClient, "productServiceUrl", PRODUCT_SERVICE_URL);
    }

    @Test
    @DisplayName("상품 동기화 데이터를 정상적으로 조회한다")
    void getProductsForSync_Success() {
        // Given
        int page = 0;
        int size = 100;

        PageResponse<CatalogSyncProductResponse> mockPageResponse = createMockPageResponse();
        ResponseEntity<PageResponse<CatalogSyncProductResponse>> mockResponseEntity =
                ResponseEntity.ok(mockPageResponse);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponseEntity);

        // When
        PageResponse<CatalogSyncProductResponse> result = productServiceClient.getProductsForSync(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();

        verify(restTemplate).exchange(
                contains("/api/internal/products/sync"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );
    }

    @Test
    @DisplayName("빈 페이지 응답을 정상적으로 처리한다")
    void getProductsForSync_EmptyPage() {
        // Given
        int page = 5;
        int size = 100;

        PageResponse<CatalogSyncProductResponse> mockPageResponse = createEmptyPageResponse();
        ResponseEntity<PageResponse<CatalogSyncProductResponse>> mockResponseEntity =
                ResponseEntity.ok(mockPageResponse);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponseEntity);

        // When
        PageResponse<CatalogSyncProductResponse> result = productServiceClient.getProductsForSync(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("올바른 URL로 요청을 보낸다")
    void getProductsForSync_CorrectUrl() {
        // Given
        int page = 2;
        int size = 50;
        String expectedUrl = PRODUCT_SERVICE_URL + "/api/internal/products/sync?page=2&size=50";

        PageResponse<CatalogSyncProductResponse> mockPageResponse = createEmptyPageResponse();
        ResponseEntity<PageResponse<CatalogSyncProductResponse>> mockResponseEntity =
                ResponseEntity.ok(mockPageResponse);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponseEntity);

        // When
        productServiceClient.getProductsForSync(page, size);

        // Then
        verify(restTemplate).exchange(
                eq(expectedUrl),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        );
    }

    private PageResponse<CatalogSyncProductResponse> createMockPageResponse() {
        try {
            java.lang.reflect.Constructor<PageResponse> constructor =
                    (java.lang.reflect.Constructor<PageResponse>) PageResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PageResponse<CatalogSyncProductResponse> pageResponse = constructor.newInstance();

            ReflectionTestUtils.setField(pageResponse, "content", List.of(
                    createMockProduct(1L, "Product 1"),
                    createMockProduct(2L, "Product 2")
            ));
            ReflectionTestUtils.setField(pageResponse, "page", 0);
            ReflectionTestUtils.setField(pageResponse, "size", 100);
            ReflectionTestUtils.setField(pageResponse, "totalElements", 2L);
            ReflectionTestUtils.setField(pageResponse, "totalPages", 1);
            ReflectionTestUtils.setField(pageResponse, "first", true);
            ReflectionTestUtils.setField(pageResponse, "last", true);
            return pageResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock PageResponse", e);
        }
    }

    private PageResponse<CatalogSyncProductResponse> createEmptyPageResponse() {
        try {
            java.lang.reflect.Constructor<PageResponse> constructor =
                    (java.lang.reflect.Constructor<PageResponse>) PageResponse.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            PageResponse<CatalogSyncProductResponse> pageResponse = constructor.newInstance();

            ReflectionTestUtils.setField(pageResponse, "content", List.of());
            ReflectionTestUtils.setField(pageResponse, "page", 5);
            ReflectionTestUtils.setField(pageResponse, "size", 100);
            ReflectionTestUtils.setField(pageResponse, "totalElements", 0L);
            ReflectionTestUtils.setField(pageResponse, "totalPages", 0);
            ReflectionTestUtils.setField(pageResponse, "first", false);
            ReflectionTestUtils.setField(pageResponse, "last", true);
            return pageResponse;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create empty PageResponse", e);
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

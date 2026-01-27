package com.example.productservice.product.service;

import com.example.productservice.category.domain.Category;
import com.example.productservice.category.repository.CategoryRepository;
import com.example.productservice.file.service.FileStorageService;
import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductImage;
import com.example.productservice.product.domain.ProductSearchKeyword;
import com.example.productservice.product.dto.CatalogSyncProductResponse;
import com.example.productservice.product.dto.CatalogSyncRequest;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSearchKeywordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("카탈로그 동기화 서비스 테스트")
class CatalogSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductSearchKeywordRepository productSearchKeywordRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product1;
    private Product product2;
    private Category category1;
    private ProductSearchKeyword keyword1;
    private ProductSearchKeyword keyword2;
    private ProductSearchKeyword keyword3;

    @BeforeEach
    void setUp() {
        // 카테고리 설정
        category1 = Category.builder()
                .categoryId(1L)
                .categoryName("운동화")
                .build();

        // 상품 1 설정
        product1 = Product.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .images(new ArrayList<>())
                .categories(new HashSet<>())
                .searchKeywords(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProductImage image1 = ProductImage.builder()
                .imageId(1L)
                .product(product1)
                .fileId(100L)
                .imageUrl("https://example.com/nike1.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        product1.getImages().add(image1);
        product1.getCategories().add(category1);

        // 상품 2 설정
        product2 = Product.builder()
                .productId(2L)
                .productName("아디다스 울트라부스트")
                .productCode("ADIDAS-001")
                .description("러닝화")
                .basePrice(new BigDecimal("200000"))
                .salePrice(new BigDecimal("180000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .images(new ArrayList<>())
                .categories(new HashSet<>())
                .searchKeywords(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 검색 키워드 설정
        keyword1 = ProductSearchKeyword.builder()
                .keywordId(1L)
                .product(product1)
                .keyword("운동화")
                .createdAt(LocalDateTime.now())
                .build();

        keyword2 = ProductSearchKeyword.builder()
                .keywordId(2L)
                .product(product1)
                .keyword("나이키")
                .createdAt(LocalDateTime.now())
                .build();

        keyword3 = ProductSearchKeyword.builder()
                .keywordId(3L)
                .product(product2)
                .keyword("러닝화")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getProductsForCatalogSync")
    class GetProductsForCatalogSync {

        @Test
        @DisplayName("성공 - 검색 키워드 포함하여 조회")
        void getProductsForCatalogSync_withSearchKeywords() {
            // given
            CatalogSyncRequest request = CatalogSyncRequest.builder()
                    .page(0)
                    .size(10)
                    .build();

            List<Product> products = List.of(product1, product2);
            Page<Product> productPage = new PageImpl<>(products);

            when(productRepository.findActiveDisplayedProductsWithDetails(any(Pageable.class)))
                    .thenReturn(productPage);
            when(productSearchKeywordRepository.findByProductProductIdIn(anyList()))
                    .thenReturn(List.of(keyword1, keyword2, keyword3));

            // when
            PageResponse<CatalogSyncProductResponse> result = productService.getProductsForCatalogSync(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);

            // 상품 1 검증 - 키워드 2개
            CatalogSyncProductResponse response1 = result.getContent().get(0);
            assertThat(response1.getProductId()).isEqualTo(1L);
            assertThat(response1.getProductName()).isEqualTo("나이키 에어맥스");
            assertThat(response1.getSearchKeywords()).containsExactlyInAnyOrder("운동화", "나이키");
            assertThat(response1.getPrimaryImageUrl()).isEqualTo("https://example.com/nike1.jpg");
            assertThat(response1.getCategoryIds()).contains(1L);

            // 상품 2 검증 - 키워드 1개
            CatalogSyncProductResponse response2 = result.getContent().get(1);
            assertThat(response2.getProductId()).isEqualTo(2L);
            assertThat(response2.getProductName()).isEqualTo("아디다스 울트라부스트");
            assertThat(response2.getSearchKeywords()).containsExactly("러닝화");

            verify(productRepository).findActiveDisplayedProductsWithDetails(any(Pageable.class));
            verify(productSearchKeywordRepository).findByProductProductIdIn(List.of(1L, 2L));
        }

        @Test
        @DisplayName("성공 - 키워드가 없는 상품")
        void getProductsForCatalogSync_withoutKeywords() {
            // given
            CatalogSyncRequest request = CatalogSyncRequest.builder()
                    .page(0)
                    .size(10)
                    .build();

            List<Product> products = List.of(product2);
            Page<Product> productPage = new PageImpl<>(products);

            when(productRepository.findActiveDisplayedProductsWithDetails(any(Pageable.class)))
                    .thenReturn(productPage);
            when(productSearchKeywordRepository.findByProductProductIdIn(anyList()))
                    .thenReturn(List.of()); // 키워드 없음

            // when
            PageResponse<CatalogSyncProductResponse> result = productService.getProductsForCatalogSync(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSearchKeywords()).isEmpty();
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        void getProductsForCatalogSync_emptyResult() {
            // given
            CatalogSyncRequest request = CatalogSyncRequest.builder()
                    .page(0)
                    .size(10)
                    .build();

            Page<Product> emptyPage = new PageImpl<>(List.of());

            when(productRepository.findActiveDisplayedProductsWithDetails(any(Pageable.class)))
                    .thenReturn(emptyPage);
            when(productSearchKeywordRepository.findByProductProductIdIn(anyList()))
                    .thenReturn(List.of());

            // when
            PageResponse<CatalogSyncProductResponse> result = productService.getProductsForCatalogSync(request);

            // then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공 - 페이지네이션 적용")
        void getProductsForCatalogSync_withPagination() {
            // given
            CatalogSyncRequest request = CatalogSyncRequest.builder()
                    .page(1)
                    .size(1)
                    .build();

            List<Product> products = List.of(product2);
            Page<Product> productPage = new PageImpl<>(products,
                    org.springframework.data.domain.PageRequest.of(1, 1), 2);

            when(productRepository.findActiveDisplayedProductsWithDetails(any(Pageable.class)))
                    .thenReturn(productPage);
            when(productSearchKeywordRepository.findByProductProductIdIn(anyList()))
                    .thenReturn(List.of(keyword3));

            // when
            PageResponse<CatalogSyncProductResponse> result = productService.getProductsForCatalogSync(request);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getPage()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공 - 기본 페이지 값 사용")
        void getProductsForCatalogSync_defaultPagination() {
            // given
            CatalogSyncRequest request = CatalogSyncRequest.builder().build(); // page, size 미설정

            Page<Product> productPage = new PageImpl<>(List.of(product1));

            when(productRepository.findActiveDisplayedProductsWithDetails(any(Pageable.class)))
                    .thenReturn(productPage);
            when(productSearchKeywordRepository.findByProductProductIdIn(anyList()))
                    .thenReturn(List.of(keyword1, keyword2));

            // when
            PageResponse<CatalogSyncProductResponse> result = productService.getProductsForCatalogSync(request);

            // then
            assertThat(result).isNotNull();
            verify(productRepository).findActiveDisplayedProductsWithDetails(any(Pageable.class));
        }
    }
}

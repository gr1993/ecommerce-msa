package com.example.productservice.service;

import com.example.productservice.domain.entity.Product;
import com.example.productservice.domain.entity.ProductImage;
import com.example.productservice.domain.entity.ProductSku;
import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.dto.*;
import com.example.productservice.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .productId(1L)
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        ProductSku sku1 = ProductSku.builder()
                .skuId(1L)
                .product(product1)
                .skuCode("NIKE-001-RED-250")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .build();
        product1.getSkus().add(sku1);

        ProductImage image1 = ProductImage.builder()
                .imageId(1L)
                .product(product1)
                .imageUrl("https://example.com/nike1.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        product1.getImages().add(image1);

        product2 = Product.builder()
                .productId(2L)
                .productName("아디다스 울트라부스트")
                .productCode("ADIDAS-001")
                .description("러닝화")
                .basePrice(new BigDecimal("200000"))
                .salePrice(new BigDecimal("180000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        ProductSku sku2 = ProductSku.builder()
                .skuId(2L)
                .product(product2)
                .skuCode("ADIDAS-001-BLUE-260")
                .price(new BigDecimal("180000"))
                .stockQty(30)
                .status("ACTIVE")
                .build();
        product2.getSkus().add(sku2);
    }

    @Test
    @DisplayName("상품 목록 조회 - 기본 페이지네이션")
    void searchProducts_withPagination() {
        // given
        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(2);

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 상품명 검색")
    void searchProducts_byProductName() {
        // given
        List<Product> products = List.of(product1);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("나이키")
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getProductName()).contains("나이키");

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 상태 필터")
    void searchProducts_byStatus() {
        // given
        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("ACTIVE")
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent())
                .allMatch(productResponse -> productResponse.getStatus().equals("ACTIVE"));

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 가격 범위 검색")
    void searchProducts_byPriceRange() {
        // given
        List<Product> products = List.of(product1);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(100000.0)
                .maxPrice(160000.0)
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 정렬 적용")
    void searchProducts_withSorting() {
        // given
        List<Product> products = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .sort("basePrice,desc")
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("상품 목록 조회 - 빈 결과")
    void searchProducts_emptyResult() {
        // given
        List<Product> products = List.of();
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("존재하지않는상품")
                .page(0)
                .size(10)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);

        verify(productRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("ProductResponse 변환 테스트 - 재고 합산")
    void productResponse_stockQtySum() {
        // given
        List<Product> products = List.of(product1);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder().build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response.getContent().get(0).getTotalStockQty()).isEqualTo(50);
    }

    @Test
    @DisplayName("ProductResponse 변환 테스트 - 대표 이미지")
    void productResponse_primaryImage() {
        // given
        List<Product> products = List.of(product1);
        Page<Product> productPage = new PageImpl<>(products);

        ProductSearchRequest request = ProductSearchRequest.builder().build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // when
        PageResponse<ProductResponse> response = productService.searchProducts(request);

        // then
        assertThat(response.getContent().get(0).getPrimaryImageUrl())
                .isEqualTo("https://example.com/nike1.jpg");
    }

    @Test
    @DisplayName("상품 등록 - 성공")
    void createProduct_success() {
        // given
        OptionValueRequest optionValue1 = OptionValueRequest.builder()
                .id("value_1")
                .optionValueName("Red")
                .displayOrder(0)
                .build();

        OptionValueRequest optionValue2 = OptionValueRequest.builder()
                .id("value_2")
                .optionValueName("Blue")
                .displayOrder(1)
                .build();

        OptionGroupRequest optionGroup = OptionGroupRequest.builder()
                .id("group_1")
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(optionValue1, optionValue2))
                .build();

        SkuRequest sku1 = SkuRequest.builder()
                .id("sku_1")
                .skuCode("SKU-001-RED")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1"))
                .build();

        SkuRequest sku2 = SkuRequest.builder()
                .id("sku_2")
                .skuCode("SKU-001-BLUE")
                .price(new BigDecimal("120000"))
                .stockQty(30)
                .status("ACTIVE")
                .optionValueIds(List.of("value_2"))
                .build();

        ProductImageRequest image = ProductImageRequest.builder()
                .id("img_1")
                .imageUrl("https://example.com/image.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(List.of(optionGroup))
                .skus(List.of(sku1, sku2))
                .images(List.of(image))
                .build();

        Product savedProduct = Product.builder()
                .productId(1L)
                .productName(request.getProductName())
                .productCode(request.getProductCode())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .status(request.getStatus())
                .isDisplayed(request.getIsDisplayed())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductName()).isEqualTo("나이키 에어맥스");
        assertThat(response.getProductCode()).isEqualTo("NIKE-001");
        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("150000"));

        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 등록 - 옵션 없이 등록")
    void createProduct_withoutOptions() {
        // given
        ProductCreateRequest request = ProductCreateRequest.builder()
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        Product savedProduct = Product.builder()
                .productId(1L)
                .productName(request.getProductName())
                .productCode(request.getProductCode())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .status(request.getStatus())
                .isDisplayed(request.getIsDisplayed())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductName()).isEqualTo("나이키 에어맥스");

        verify(productRepository, times(1)).save(any(Product.class));
    }
}

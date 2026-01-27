package com.example.productservice.product.service;

import com.example.productservice.category.repository.CategoryRepository;
import com.example.productservice.file.service.FileStorageService;
import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.*;
import com.example.productservice.product.dto.*;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSearchKeywordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
class ProductServiceImplTest {

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
                .fileId(100L)
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
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(List.of(optionValue1, optionValue2))
                .build();

        SkuRequest sku1 = SkuRequest.builder()
                .skuCode("SKU-001-RED")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .optionValueIds(List.of("value_1"))
                .build();

        SkuRequest sku2 = SkuRequest.builder()
                .skuCode("SKU-001-BLUE")
                .price(new BigDecimal("120000"))
                .stockQty(30)
                .status("ACTIVE")
                .optionValueIds(List.of("value_2"))
                .build();

        ProductImageRequest image = ProductImageRequest.builder()
                .fileId(1L)
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

        Map<Long, String> idUrlMap = new HashMap<>();
        idUrlMap.put(1L, "");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(fileStorageService.confirmFiles(any(List.class))).thenReturn(idUrlMap);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductName()).isEqualTo("나이키 에어맥스");
        assertThat(response.getProductCode()).isEqualTo("NIKE-001");
        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("150000"));

        verify(productRepository, times(1)).save(any(Product.class));
        verify(fileStorageService, times(1)).confirmFiles(any(List.class));
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

    // ==================== 상품 상세 조회 테스트 ====================

    @Test
    @DisplayName("상품 상세 조회 - 성공")
    void getProductDetail_success() {
        // given
        Product product = createProductWithFullDetails();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("나이키 에어맥스");
        assertThat(response.getProductCode()).isEqualTo("NIKE-001");
        assertThat(response.getDescription()).isEqualTo("편안한 운동화");
        assertThat(response.getBasePrice()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(response.getSalePrice()).isEqualByComparingTo(new BigDecimal("120000"));
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getIsDisplayed()).isTrue();

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("상품 상세 조회 - 옵션 그룹 포함")
    void getProductDetail_withOptionGroups() {
        // given
        Product product = createProductWithFullDetails();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(1L);

        // then
        assertThat(response.getOptionGroups()).hasSize(1);
        assertThat(response.getOptionGroups().get(0).getOptionGroupName()).isEqualTo("색상");
        assertThat(response.getOptionGroups().get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(response.getOptionGroups().get(0).getOptionValues()).hasSize(2);
        assertThat(response.getOptionGroups().get(0).getOptionValues().get(0).getOptionValueName()).isEqualTo("Red");
        assertThat(response.getOptionGroups().get(0).getOptionValues().get(1).getOptionValueName()).isEqualTo("Blue");

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("상품 상세 조회 - SKU 포함")
    void getProductDetail_withSkus() {
        // given
        Product product = createProductWithFullDetails();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(1L);

        // then
        assertThat(response.getSkus()).hasSize(2);
        assertThat(response.getSkus().get(0).getSkuCode()).isEqualTo("SKU-001-RED");
        assertThat(response.getSkus().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("120000"));
        assertThat(response.getSkus().get(0).getStockQty()).isEqualTo(50);
        assertThat(response.getSkus().get(0).getStatus()).isEqualTo("ACTIVE");

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("상품 상세 조회 - 이미지 포함")
    void getProductDetail_withImages() {
        // given
        Product product = createProductWithFullDetails();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(1L);

        // then
        assertThat(response.getImages()).hasSize(1);
        assertThat(response.getImages().get(0).getFileId()).isEqualTo(100L);
        assertThat(response.getImages().get(0).getImageUrl()).isEqualTo("https://example.com/nike1.jpg");
        assertThat(response.getImages().get(0).getIsPrimary()).isTrue();
        assertThat(response.getImages().get(0).getDisplayOrder()).isEqualTo(0);

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("상품 상세 조회 - 존재하지 않는 상품")
    void getProductDetail_notFound() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProductDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");

        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("상품 상세 조회 - 옵션 없는 상품")
    void getProductDetail_withoutOptions() {
        // given
        Product product = Product.builder()
                .productId(2L)
                .productName("옵션 없는 상품")
                .productCode("NO-OPT-001")
                .description("옵션이 없는 단순 상품")
                .basePrice(new BigDecimal("50000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        when(productRepository.findById(2L)).thenReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProductDetail(2L);

        // then
        assertThat(response.getProductId()).isEqualTo(2L);
        assertThat(response.getProductName()).isEqualTo("옵션 없는 상품");
        assertThat(response.getOptionGroups()).isEmpty();
        assertThat(response.getSkus()).isEmpty();
        assertThat(response.getImages()).isEmpty();

        verify(productRepository, times(1)).findById(2L);
    }

    // ==================== 상품 수정 테스트 ====================

    @Test
    @DisplayName("상품 수정 - 성공")
    void updateProduct_success() {
        // given
        Product existingProduct = createProductWithFullDetails();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        ProductCreateRequest updateRequest = ProductCreateRequest.builder()
                .productName("수정된 상품명")
                .productCode("UPDATED-001")
                .description("수정된 설명")
                .basePrice(new BigDecimal("200000"))
                .salePrice(new BigDecimal("180000"))
                .status("INACTIVE")
                .isDisplayed(false)
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        // when
        ProductResponse response = productService.updateProduct(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 수정 - 존재하지 않는 상품")
    void updateProduct_notFound() {
        // given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductCreateRequest updateRequest = ProductCreateRequest.builder()
                .productName("수정된 상품명")
                .basePrice(new BigDecimal("200000"))
                .status("ACTIVE")
                .optionGroups(new ArrayList<>())
                .skus(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(999L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    private Product createProductWithFullDetails() {
        Product product = Product.builder()
                .productId(1L)
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

        // 옵션 그룹 추가
        ProductOptionGroup optionGroup = ProductOptionGroup.builder()
                .optionGroupId(1L)
                .product(product)
                .optionGroupName("색상")
                .displayOrder(0)
                .optionValues(new ArrayList<>())
                .build();
        product.getOptionGroups().add(optionGroup);

        // 옵션 값 추가
        ProductOptionValue optionValue1 = ProductOptionValue.builder()
                .optionValueId(1L)
                .optionGroup(optionGroup)
                .optionValueName("Red")
                .displayOrder(0)
                .skuOptions(new ArrayList<>())
                .build();
        optionGroup.getOptionValues().add(optionValue1);

        ProductOptionValue optionValue2 = ProductOptionValue.builder()
                .optionValueId(2L)
                .optionGroup(optionGroup)
                .optionValueName("Blue")
                .displayOrder(1)
                .skuOptions(new ArrayList<>())
                .build();
        optionGroup.getOptionValues().add(optionValue2);

        // SKU 추가
        ProductSku sku1 = ProductSku.builder()
                .skuId(1L)
                .product(product)
                .skuCode("SKU-001-RED")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .skuOptions(new ArrayList<>())
                .build();
        product.getSkus().add(sku1);

        ProductSku sku2 = ProductSku.builder()
                .skuId(2L)
                .product(product)
                .skuCode("SKU-001-BLUE")
                .price(new BigDecimal("120000"))
                .stockQty(30)
                .status("ACTIVE")
                .skuOptions(new ArrayList<>())
                .build();
        product.getSkus().add(sku2);

        // SKU-옵션 연결
        ProductSkuOption skuOption1 = ProductSkuOption.builder()
                .skuOptionId(1L)
                .sku(sku1)
                .optionValue(optionValue1)
                .build();
        sku1.getSkuOptions().add(skuOption1);

        ProductSkuOption skuOption2 = ProductSkuOption.builder()
                .skuOptionId(2L)
                .sku(sku2)
                .optionValue(optionValue2)
                .build();
        sku2.getSkuOptions().add(skuOption2);

        // 이미지 추가
        ProductImage image = ProductImage.builder()
                .imageId(1L)
                .product(product)
                .fileId(100L)
                .imageUrl("https://example.com/nike1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();
        product.getImages().add(image);

        return product;
    }
}

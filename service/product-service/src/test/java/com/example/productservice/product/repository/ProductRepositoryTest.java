package com.example.productservice.product.repository;

import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductImage;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.dto.ProductSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("ProductRepository 테스트")
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        product1 = Product.builder()
                .productName("나이키 에어맥스")
                .productCode("NIKE-001")
                .description("편안한 운동화")
                .basePrice(new BigDecimal("150000"))
                .salePrice(new BigDecimal("120000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .build();

        ProductSku sku1 = ProductSku.builder()
                .product(product1)
                .skuCode("NIKE-001-RED-250")
                .price(new BigDecimal("120000"))
                .stockQty(50)
                .status("ACTIVE")
                .build();
        product1.getSkus().add(sku1);

        ProductImage image1 = ProductImage.builder()
                .product(product1)
                .imageUrl("https://example.com/nike1.jpg")
                .isPrimary(true)
                .displayOrder(1)
                .build();
        product1.getImages().add(image1);

        product2 = Product.builder()
                .productName("아디다스 울트라부스트")
                .productCode("ADIDAS-001")
                .description("러닝화")
                .basePrice(new BigDecimal("200000"))
                .salePrice(new BigDecimal("180000"))
                .status("ACTIVE")
                .isDisplayed(true)
                .build();

        ProductSku sku2 = ProductSku.builder()
                .product(product2)
                .skuCode("ADIDAS-001-BLUE-260")
                .price(new BigDecimal("180000"))
                .stockQty(30)
                .status("ACTIVE")
                .build();
        product2.getSkus().add(sku2);

        product3 = Product.builder()
                .productName("뉴발란스 530")
                .productCode("NB-001")
                .description("캐주얼 운동화")
                .basePrice(new BigDecimal("100000"))
                .salePrice(new BigDecimal("90000"))
                .status("INACTIVE")
                .isDisplayed(false)
                .build();

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
    }

    @Test
    @DisplayName("상품 코드로 상품 조회")
    void findByProductCode() {
        // when
        Product found = productRepository.findByProductCode("NIKE-001").orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getProductName()).isEqualTo("나이키 에어맥스");
        assertThat(found.getProductCode()).isEqualTo("NIKE-001");
    }

    @Test
    @DisplayName("상품명으로 검색")
    void searchByProductName() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName("나이키")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).contains("나이키");
    }

    @Test
    @DisplayName("상태로 검색")
    void searchByStatus() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("ACTIVE")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(product -> product.getStatus().equals("ACTIVE"));
    }

    @Test
    @DisplayName("진열 여부로 검색")
    void searchByIsDisplayed() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .isDisplayed(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(Product::getIsDisplayed);
    }

    @Test
    @DisplayName("가격 범위로 검색")
    void searchByPriceRange() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .minPrice(100000.0)
                .maxPrice(180000.0)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .allMatch(product ->
                        product.getBasePrice().compareTo(new BigDecimal("100000")) >= 0 &&
                        product.getBasePrice().compareTo(new BigDecimal("180000")) <= 0
                );
    }

    @Test
    @DisplayName("복합 조건으로 검색")
    void searchByMultipleConditions() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder()
                .status("ACTIVE")
                .isDisplayed(true)
                .minPrice(100000.0)
                .maxPrice(180000.0)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("나이키 에어맥스");
    }

    @Test
    @DisplayName("페이지네이션 테스트")
    void pagination() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 2);

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    @DisplayName("정렬 테스트 - 가격 내림차순")
    void sortByPriceDesc() {
        // given
        ProductSearchRequest request = ProductSearchRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("basePrice").descending());

        // when
        Page<Product> result = productRepository.findAll(
                ProductSpecification.searchWith(request),
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getBasePrice())
                .isGreaterThanOrEqualTo(result.getContent().get(1).getBasePrice());
        assertThat(result.getContent().get(1).getBasePrice())
                .isGreaterThanOrEqualTo(result.getContent().get(2).getBasePrice());
    }
}

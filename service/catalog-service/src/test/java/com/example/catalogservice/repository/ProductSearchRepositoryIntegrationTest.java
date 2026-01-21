package com.example.catalogservice.repository;

import com.example.catalogservice.config.ElasticsearchTestContainerConfig;
import com.example.catalogservice.domain.document.ProductDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "springwolf.enabled=false"
})
@Import(ElasticsearchTestContainerConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductSearchRepositoryIntegrationTest {

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUp() {
        // 다른 테스트에서 인덱스를 삭제했을 수 있으므로, 인덱스가 없으면 생성
        IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
        productSearchRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("상품 문서 저장 테스트")
    void saveProductDocument() {
        // given
        ProductDocument product = createProductDocument("1", "맥북 프로 16인치", "애플 M3 맥스 칩 탑재");

        // when
        ProductDocument saved = productSearchRepository.save(product);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getProductId()).isEqualTo("1");
        assertThat(saved.getProductName()).isEqualTo("맥북 프로 16인치");
    }

    @Test
    @Order(2)
    @DisplayName("상품 문서 조회 테스트")
    void findProductById() {
        // given
        ProductDocument product = createProductDocument("2", "갤럭시 S24 울트라", "삼성 최신 플래그십 스마트폰");
        productSearchRepository.save(product);

        // when
        Optional<ProductDocument> found = productSearchRepository.findById("2");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProductName()).isEqualTo("갤럭시 S24 울트라");
        assertThat(found.get().getDescription()).isEqualTo("삼성 최신 플래그십 스마트폰");
    }

    @Test
    @Order(3)
    @DisplayName("상품 문서 삭제 테스트")
    void deleteProductDocument() {
        // given
        ProductDocument product = createProductDocument("3", "에어팟 프로 2세대", "노이즈 캔슬링 이어폰");
        productSearchRepository.save(product);

        // when
        productSearchRepository.deleteById("3");

        // then
        Optional<ProductDocument> found = productSearchRepository.findById("3");
        assertThat(found).isEmpty();
    }

    @Test
    @Order(4)
    @DisplayName("여러 상품 문서 저장 및 조회 테스트")
    void saveAndFindAllProducts() {
        // given
        List<ProductDocument> products = List.of(
                createProductDocument("4", "아이패드 프로 12.9", "M2 칩 탑재 태블릿"),
                createProductDocument("5", "갤럭시 탭 S9", "안드로이드 태블릿"),
                createProductDocument("6", "서피스 프로 9", "윈도우 태블릿")
        );
        productSearchRepository.saveAll(products);

        // when
        Iterable<ProductDocument> allProducts = productSearchRepository.findAll();

        // then
        assertThat(allProducts).hasSize(3);
    }

    @Test
    @Order(5)
    @DisplayName("상품 문서 업데이트 테스트")
    void updateProductDocument() {
        // given
        ProductDocument product = createProductDocument("7", "LG 그램 17", "초경량 노트북");
        productSearchRepository.save(product);

        ProductDocument updatedProduct = ProductDocument.builder()
                .productId("7")
                .productName("LG 그램 17 2024")
                .description("초경량 노트북 신형")
                .basePrice(2500000L)
                .salePrice(2300000L)
                .status("ON_SALE")
                .createdAt(product.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        // when
        ProductDocument saved = productSearchRepository.save(updatedProduct);

        // then
        Optional<ProductDocument> found = productSearchRepository.findById("7");
        assertThat(found).isPresent();
        assertThat(found.get().getProductName()).isEqualTo("LG 그램 17 2024");
        assertThat(found.get().getDescription()).isEqualTo("초경량 노트북 신형");
    }

    @Test
    @Order(6)
    @DisplayName("인덱스 존재 여부 확인 테스트")
    void checkIndexExists() {
        // given
        ProductDocument product = createProductDocument("8", "테스트 상품", "테스트 설명");
        productSearchRepository.save(product);

        // when
        boolean exists = elasticsearchOperations.indexOps(ProductDocument.class).exists();

        // then
        assertThat(exists).isTrue();
    }

    private ProductDocument createProductDocument(String id, String name, String description) {
        return ProductDocument.builder()
                .productId(id)
                .productName(name)
                .description(description)
                .basePrice(1000000L)
                .salePrice(900000L)
                .status("ON_SALE")
                .primaryImageUrl("https://example.com/image.jpg")
                .categoryIds(List.of(1L, 2L))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

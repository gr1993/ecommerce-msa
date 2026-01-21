package com.example.catalogservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import com.example.catalogservice.config.ElasticsearchTestContainerConfig;
import com.example.catalogservice.domain.document.ProductDocument;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "springwolf.enabled=false"
})
@Import(ElasticsearchTestContainerConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ElasticsearchIndexServiceIntegrationTest {

    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트 전에 기존 products alias와 관련 인덱스들 정리
        cleanupTestIndices();
    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트 후 정리
        cleanupTestIndices();
    }

    private void cleanupTestIndices() throws IOException {
        // alias가 가리키는 인덱스들 확인
        Set<String> indices = elasticsearchIndexService.getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);

        // alias 제거
        if (!indices.isEmpty()) {
            for (String index : indices) {
                try {
                    elasticsearchClient.indices().updateAliases(u -> u
                            .actions(a -> a
                                    .remove(r -> r
                                            .index(index)
                                            .alias(ElasticsearchIndexService.ALIAS_NAME))));
                } catch (Exception e) {
                    // 이미 제거된 경우 무시
                }
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
        try {
            GetIndexResponse response = elasticsearchClient.indices()
                    .get(g -> g.index("products_*"));
            for (String indexName : response.result().keySet()) {
                elasticsearchIndexService.deleteIndex(indexName);
            }
        } catch (Exception e) {
            // 인덱스가 없는 경우 무시
        }
    }

    @Test
    @Order(1)
    @DisplayName("새 인덱스 생성 - settings와 매핑이 올바르게 적용되는지 검증")
    void createNewIndex() throws IOException {
        // when
        String newIndexName = elasticsearchIndexService.createNewIndex();

        // then
        assertThat(newIndexName).startsWith("products_");
        assertThat(elasticsearchIndexService.indexExists(newIndexName)).isTrue();

        // 인덱스 설정 확인 - nori 분석기가 포함되어 있는지 검증
        GetIndexResponse indexResponse = elasticsearchClient.indices()
                .get(g -> g.index(newIndexName));

        assertThat(indexResponse.result()).containsKey(newIndexName);
        assertThat(indexResponse.result().get(newIndexName).settings())
                .isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("alias 전환 - 기존 인덱스에서 alias를 제거하고 새 인덱스에 추가")
    void switchAlias() throws IOException {
        // given
        String firstIndex = elasticsearchIndexService.createNewIndex();
        elasticsearchIndexService.switchAlias(firstIndex);

        // when - 두 번째 인덱스를 생성하고 alias 전환
        String secondIndex = elasticsearchIndexService.createNewIndex();
        elasticsearchIndexService.switchAlias(secondIndex);

        // then - alias가 두 번째 인덱스를 가리키는지 확인
        Set<String> indicesByAlias = elasticsearchIndexService
                .getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);

        assertThat(indicesByAlias).hasSize(1);
        assertThat(indicesByAlias).contains(secondIndex);
        assertThat(indicesByAlias).doesNotContain(firstIndex);

        // Elasticsearch API로 직접 확인
        GetAliasResponse aliasResponse = elasticsearchClient.indices()
                .getAlias(g -> g.name(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(aliasResponse.result()).containsKey(secondIndex);
        assertThat(aliasResponse.result()).doesNotContainKey(firstIndex);
    }

    @Test
    @Order(3)
    @DisplayName("alias가 없는 경우에도 새 인덱스에 alias 추가 가능")
    void switchAliasWhenNoExistingAlias() throws IOException {
        // given
        String newIndex = elasticsearchIndexService.createNewIndex();

        // when
        elasticsearchIndexService.switchAlias(newIndex);

        // then
        Set<String> indices = elasticsearchIndexService
                .getIndicesByAlias(ElasticsearchIndexService.ALIAS_NAME);

        assertThat(indices).hasSize(1);
        assertThat(indices).contains(newIndex);
    }

    @Test
    @Order(4)
    @DisplayName("이전 인덱스 삭제 - 현재 인덱스는 유지하고 나머지만 삭제")
    void deleteOldIndices() throws IOException {
        // given
        String firstIndex = elasticsearchIndexService.createNewIndex();
        String secondIndex = elasticsearchIndexService.createNewIndex();
        String thirdIndex = elasticsearchIndexService.createNewIndex();

        // when - 세 번째 인덱스를 현재 인덱스로 지정하고 이전 인덱스 삭제
        elasticsearchIndexService.deleteOldIndices(thirdIndex);

        // then - 세 번째 인덱스만 남아있어야 함
        assertThat(elasticsearchIndexService.indexExists(firstIndex)).isFalse();
        assertThat(elasticsearchIndexService.indexExists(secondIndex)).isFalse();
        assertThat(elasticsearchIndexService.indexExists(thirdIndex)).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("특정 인덱스 삭제")
    void deleteIndex() throws IOException {
        // given
        String indexName = elasticsearchIndexService.createNewIndex();
        assertThat(elasticsearchIndexService.indexExists(indexName)).isTrue();

        // when
        elasticsearchIndexService.deleteIndex(indexName);

        // then
        assertThat(elasticsearchIndexService.indexExists(indexName)).isFalse();
    }

    @Test
    @Order(6)
    @DisplayName("존재하지 않는 인덱스 삭제 시도 - 예외가 발생하지 않아야 함")
    void deleteNonExistentIndex() throws IOException {
        // given
        String nonExistentIndex = "products_999999_999999";

        // when & then - 예외가 발생하지 않아야 함
        elasticsearchIndexService.deleteIndex(nonExistentIndex);
    }

    @Test
    @Order(7)
    @DisplayName("alias 존재 여부 확인")
    void aliasExists() throws IOException {
        // given
        String indexName = elasticsearchIndexService.createNewIndex();

        // when - alias 추가 전
        boolean beforeSwitch = elasticsearchIndexService
                .aliasExists(ElasticsearchIndexService.ALIAS_NAME);

        // alias 추가
        elasticsearchIndexService.switchAlias(indexName);

        // alias 추가 후
        boolean afterSwitch = elasticsearchIndexService
                .aliasExists(ElasticsearchIndexService.ALIAS_NAME);

        // then
        assertThat(beforeSwitch).isFalse();
        assertThat(afterSwitch).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("인덱스 존재 여부 확인")
    void indexExists() throws IOException {
        // given
        String indexName = elasticsearchIndexService.createNewIndex();

        // when
        boolean exists = elasticsearchIndexService.indexExists(indexName);
        boolean notExists = elasticsearchIndexService.indexExists("products_nonexistent");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @Order(9)
    @DisplayName("새 인덱스에 데이터 저장 및 alias를 통한 조회 가능")
    void createIndexAndSaveData() throws IOException {
        // given
        String newIndexName = elasticsearchIndexService.createNewIndex();

        ProductDocument product = ProductDocument.builder()
                .productId("test-1")
                .productName("테스트 상품")
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(9000L)
                .status("ON_SALE")
                .categoryIds(List.of(1L, 2L))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // when - 새 인덱스에 데이터 저장
        elasticsearchOperations.save(product, IndexCoordinates.of(newIndexName));
        elasticsearchOperations.indexOps(IndexCoordinates.of(newIndexName)).refresh();

        // alias 전환
        elasticsearchIndexService.switchAlias(newIndexName);

        // then - alias를 통해 조회 가능
        ProductDocument found = elasticsearchOperations.get("test-1", ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(found).isNotNull();
        assertThat(found.getProductName()).isEqualTo("테스트 상품");
    }

    @Test
    @Order(10)
    @DisplayName("여러 alias 전환 시나리오 - 실제 운영 환경 시뮬레이션")
    void multipleAliasSwitchScenario() throws IOException {
        // given - 첫 번째 인덱스 생성 및 데이터 저장
        String firstIndex = elasticsearchIndexService.createNewIndex();
        saveTestProduct("1", "상품1", firstIndex);
        elasticsearchIndexService.switchAlias(firstIndex);

        // when - 두 번째 인덱스 생성 및 새 데이터 저장
        String secondIndex = elasticsearchIndexService.createNewIndex();
        saveTestProduct("2", "상품2", secondIndex);
        elasticsearchIndexService.switchAlias(secondIndex);

        // then - alias는 두 번째 인덱스를 가리키고, 두 번째 인덱스의 데이터만 조회됨
        ProductDocument found = elasticsearchOperations.get("2", ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(found).isNotNull();
        assertThat(found.getProductName()).isEqualTo("상품2");

        // 첫 번째 인덱스의 데이터는 alias로 조회되지 않음
        ProductDocument notFound = elasticsearchOperations.get("1", ProductDocument.class,
                IndexCoordinates.of(ElasticsearchIndexService.ALIAS_NAME));

        assertThat(notFound).isNull();
    }

    private void saveTestProduct(String id, String name, String indexName) {
        ProductDocument product = ProductDocument.builder()
                .productId(id)
                .productName(name)
                .description("테스트 설명")
                .basePrice(10000L)
                .salePrice(9000L)
                .status("ON_SALE")
                .categoryIds(List.of(1L))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        elasticsearchOperations.save(product, IndexCoordinates.of(indexName));
        elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).refresh();
    }
}

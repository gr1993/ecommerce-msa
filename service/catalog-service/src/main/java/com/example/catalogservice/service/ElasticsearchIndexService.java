package com.example.catalogservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.elasticsearch.indices.update_aliases.RemoveAction;
import com.example.catalogservice.domain.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchIndexService {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchOperations elasticsearchOperations;

    public static final String ALIAS_NAME = "products";
    private static final String INDEX_PREFIX = "products_";
    private static final DateTimeFormatter INDEX_SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS");

    /**
     * 새 인덱스를 생성하고 인덱스 이름을 반환한다.
     * settings.json과 ProductDocument의 매핑을 적용한다.
     */
    public String createNewIndex() throws IOException {
        String newIndexName = generateIndexName();

        IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(newIndexName));

        // ProductDocument의 설정과 매핑을 사용하여 인덱스 생성
        IndexOperations productIndexOps = elasticsearchOperations.indexOps(ProductDocument.class);
        Map<String, Object> settings = productIndexOps.createSettings();
        org.springframework.data.elasticsearch.core.document.Document mapping = productIndexOps.createMapping();

        indexOps.create(settings, mapping);

        log.info("Created new index: {}", newIndexName);
        return newIndexName;
    }

    /**
     * alias를 새 인덱스로 전환한다.
     * 기존 인덱스에서 alias를 제거하고 새 인덱스에 alias를 추가한다.
     */
    public void switchAlias(String newIndexName) throws IOException {
        Set<String> oldIndices = getIndicesByAlias(ALIAS_NAME);

        UpdateAliasesRequest.Builder requestBuilder = new UpdateAliasesRequest.Builder();

        // 기존 인덱스에서 alias 제거
        for (String oldIndex : oldIndices) {
            requestBuilder.actions(Action.of(a -> a
                    .remove(RemoveAction.of(r -> r
                            .index(oldIndex)
                            .alias(ALIAS_NAME)))));
            log.info("Removing alias '{}' from index '{}'", ALIAS_NAME, oldIndex);
        }

        // 새 인덱스에 alias 추가
        requestBuilder.actions(Action.of(a -> a
                .add(AddAction.of(add -> add
                        .index(newIndexName)
                        .alias(ALIAS_NAME)))));
        log.info("Adding alias '{}' to index '{}'", ALIAS_NAME, newIndexName);

        elasticsearchClient.indices().updateAliases(requestBuilder.build());
        log.info("Alias '{}' switched to index '{}'", ALIAS_NAME, newIndexName);
    }

    /**
     * 오래된 인덱스들을 삭제한다.
     * 현재 alias가 가리키는 인덱스는 삭제하지 않는다.
     */
    public void deleteOldIndices(String currentIndexName) throws IOException {
        GetIndexResponse response = elasticsearchClient.indices().get(g -> g.index(INDEX_PREFIX + "*"));

        for (String indexName : response.result().keySet()) {
            if (!indexName.equals(currentIndexName)) {
                deleteIndex(indexName);
            }
        }
    }

    /**
     * 특정 인덱스를 삭제한다.
     */
    public void deleteIndex(String indexName) throws IOException {
        boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
        if (exists) {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
            log.info("Deleted index: {}", indexName);
        }
    }

    /**
     * alias가 가리키는 인덱스 목록을 반환한다.
     */
    public Set<String> getIndicesByAlias(String aliasName) throws IOException {
        try {
            GetAliasResponse response = elasticsearchClient.indices().getAlias(g -> g.name(aliasName));
            return response.result().keySet();
        } catch (Exception e) {
            log.debug("Alias '{}' does not exist or has no indices", aliasName);
            return Set.of();
        }
    }

    /**
     * 인덱스 존재 여부를 확인한다.
     */
    public boolean indexExists(String indexName) throws IOException {
        return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
    }

    /**
     * alias 존재 여부를 확인한다.
     */
    public boolean aliasExists(String aliasName) throws IOException {
        return elasticsearchClient.indices().existsAlias(e -> e.name(aliasName)).value();
    }

    private String generateIndexName() {
        return INDEX_PREFIX + LocalDateTime.now().format(INDEX_SUFFIX_FORMATTER);
    }
}

package com.example.catalogservice.infrastructure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final String ALIAS_NAME = "products";

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            // 1. 별칭(Alias)이 존재하는지 확인
            boolean aliasExists = elasticsearchClient.indices()
                    .existsAlias(e -> e.name(ALIAS_NAME))
                    .value();

            if (!aliasExists) {
                log.info("Alias '{}' not found. Performing initial setup...", ALIAS_NAME);

                // 2. 초기 실제 인덱스 생성 (예: products_initial)
                String initialIndexName = ALIAS_NAME + "_initial";
                IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(initialIndexName));
                if (!indexOps.exists()) {
                    IndexOperations productIndexOps = elasticsearchOperations.indexOps(ProductDocument.class);
                    indexOps.create(productIndexOps.createSettings(), productIndexOps.createMapping());
                    log.info("Created initial index: {}", initialIndexName);
                }

                // 3. 별칭 추가
                UpdateAliasesRequest request = UpdateAliasesRequest.of(rb -> rb
                        .actions(a -> a.add(add -> add
                                .index(initialIndexName)
                                .alias(ALIAS_NAME)))
                );

                elasticsearchClient.indices().updateAliases(request);
                log.info("Successfully linked alias '{}' to index '{}'", ALIAS_NAME, initialIndexName);
            }
        } catch (IOException e) {
            log.error("Failed to initialize Elasticsearch index/alias", e);
        }
    }
}
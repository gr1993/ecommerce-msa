package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.domain.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductServiceClient productServiceClient;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchIndexService elasticsearchIndexService;

    private static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * alias 기반 reindex를 통한 Full Sync.
     * 1. 새 인덱스 생성
     * 2. 새 인덱스에 데이터 동기화
     * 3. alias를 새 인덱스로 전환
     * 4. 이전 인덱스 삭제
     *
     * 이 방식은 삭제된 상품도 자동으로 처리된다 (새 인덱스에는 현재 존재하는 상품만 포함).
     */
    public int fullSync() {
        log.info("Starting full sync with alias-based reindex strategy");

        String newIndexName = null;
        try {
            // 1. 새 인덱스 생성
            newIndexName = elasticsearchIndexService.createNewIndex();
            IndexCoordinates indexCoordinates = IndexCoordinates.of(newIndexName);

            // 2. 새 인덱스에 데이터 동기화
            int totalSynced = syncToIndex(indexCoordinates);

            // 3. alias를 새 인덱스로 전환
            elasticsearchIndexService.switchAlias(newIndexName);

            // 4. 이전 인덱스 삭제
            elasticsearchIndexService.deleteOldIndices(newIndexName);

            log.info("Full sync completed successfully. Total products synced: {}", totalSynced);
            return totalSynced;

        } catch (Exception e) {
            log.error("Full sync failed", e);
            // 실패 시 새로 생성된 인덱스 정리
            if (newIndexName != null) {
                try {
                    elasticsearchIndexService.deleteIndex(newIndexName);
                } catch (IOException ex) {
                    log.error("Failed to cleanup new index: {}", newIndexName, ex);
                }
            }
            throw new RuntimeException("Full sync failed", e);
        }
    }

    private int syncToIndex(IndexCoordinates indexCoordinates) {
        int page = 0;
        int totalSynced = 0;

        while (true) {
            PageResponse<CatalogSyncProductResponse> response = productServiceClient.getProductsForSync(page, DEFAULT_PAGE_SIZE);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.info("No more products to sync at page {}", page);
                break;
            }

            List<IndexQuery> indexQueries = response.getContent().stream()
                    .map(this::toProductDocument)
                    .map(doc -> new IndexQueryBuilder()
                            .withId(doc.getProductId())
                            .withObject(doc)
                            .build())
                    .toList();

            elasticsearchOperations.bulkIndex(indexQueries, indexCoordinates);
            totalSynced += indexQueries.size();

            log.info("Synced page {} with {} products (total: {})", page, indexQueries.size(), totalSynced);

            if (response.isLast()) {
                break;
            }

            page++;
        }

        // 인덱스 refresh하여 즉시 검색 가능하도록 함
        elasticsearchOperations.indexOps(indexCoordinates).refresh();

        return totalSynced;
    }

    private ProductDocument toProductDocument(CatalogSyncProductResponse product) {
        return ProductDocument.builder()
                .productId(String.valueOf(product.getProductId()))
                .productName(product.getProductName())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .status(product.getStatus())
                .primaryImageUrl(product.getPrimaryImageUrl())
                .categoryIds(product.getCategoryIds())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}

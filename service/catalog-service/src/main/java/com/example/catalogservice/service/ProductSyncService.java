package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSyncService {

    private final ProductServiceClient productServiceClient;
    private final ProductSearchRepository productSearchRepository;

    private static final int DEFAULT_PAGE_SIZE = 100;

    public int fullSync() {
        log.info("Starting full sync from product-service to Elasticsearch");

        int page = 0;
        int totalSynced = 0;

        while (true) {
            PageResponse<CatalogSyncProductResponse> response = productServiceClient.getProductsForSync(page, DEFAULT_PAGE_SIZE);

            if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
                log.info("No more products to sync at page {}", page);
                break;
            }

            List<ProductDocument> documents = response.getContent().stream()
                    .map(this::toProductDocument)
                    .toList();

            productSearchRepository.saveAll(documents);
            totalSynced += documents.size();

            log.info("Synced page {} with {} products (total: {})", page, documents.size(), totalSynced);

            if (response.isLast()) {
                break;
            }

            page++;
        }

        log.info("Full sync completed. Total products synced: {}", totalSynced);
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

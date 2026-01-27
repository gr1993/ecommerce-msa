package com.example.catalogservice.service;

import com.example.catalogservice.consumer.event.KeywordCreatedEvent;
import com.example.catalogservice.consumer.event.KeywordDeletedEvent;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSyncService {

    private final ProductSearchRepository productSearchRepository;

    /**
     * 키워드 생성 이벤트 처리 - 상품 문서의 searchKeywords 배열에 키워드 추가
     */
    public void addKeyword(KeywordCreatedEvent event) {
        String productId = String.valueOf(event.getProductId());
        log.info("Adding keyword to product: productId={}, keyword={}", productId, event.getKeyword());

        ProductDocument document = productSearchRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found in Elasticsearch: productId=" + productId));

        List<String> keywords = document.getSearchKeywords() != null
                ? new ArrayList<>(document.getSearchKeywords())
                : new ArrayList<>();

        if (!keywords.contains(event.getKeyword())) {
            keywords.add(event.getKeyword());
        }

        ProductDocument updated = ProductDocument.builder()
                .productId(document.getProductId())
                .productName(document.getProductName())
                .description(document.getDescription())
                .basePrice(document.getBasePrice())
                .salePrice(document.getSalePrice())
                .status(document.getStatus())
                .primaryImageUrl(document.getPrimaryImageUrl())
                .categoryIds(document.getCategoryIds())
                .searchKeywords(keywords)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();

        productSearchRepository.save(updated);
        log.info("Successfully added keyword to product: productId={}, keyword={}", productId, event.getKeyword());
    }

    /**
     * 키워드 삭제 이벤트 처리 - 상품 문서의 searchKeywords 배열에서 키워드 제거
     */
    public void removeKeyword(KeywordDeletedEvent event) {
        String productId = String.valueOf(event.getProductId());
        log.info("Removing keyword from product: productId={}, keyword={}", productId, event.getKeyword());

        ProductDocument document = productSearchRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found in Elasticsearch: productId=" + productId));

        List<String> keywords = document.getSearchKeywords() != null
                ? new ArrayList<>(document.getSearchKeywords())
                : new ArrayList<>();

        keywords.remove(event.getKeyword());

        ProductDocument updated = ProductDocument.builder()
                .productId(document.getProductId())
                .productName(document.getProductName())
                .description(document.getDescription())
                .basePrice(document.getBasePrice())
                .salePrice(document.getSalePrice())
                .status(document.getStatus())
                .primaryImageUrl(document.getPrimaryImageUrl())
                .categoryIds(document.getCategoryIds())
                .searchKeywords(keywords)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();

        productSearchRepository.save(updated);
        log.info("Successfully removed keyword from product: productId={}, keyword={}", productId, event.getKeyword());
    }
}

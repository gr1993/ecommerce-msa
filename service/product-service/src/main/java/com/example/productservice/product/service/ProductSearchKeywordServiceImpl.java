package com.example.productservice.product.service;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductSearchKeyword;
import com.example.productservice.product.domain.event.KeywordCreatedEvent;
import com.example.productservice.product.domain.event.KeywordDeletedEvent;
import com.example.productservice.product.dto.SearchKeywordRequest;
import com.example.productservice.product.dto.SearchKeywordResponse;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSearchKeywordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductSearchKeywordServiceImpl implements ProductSearchKeywordService {

    private static final String AGGREGATE_TYPE_KEYWORD = "Keyword";

    private final ProductSearchKeywordRepository keywordRepository;
    private final ProductRepository productRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<SearchKeywordResponse> getKeywordsByProductId(Long productId) {
        log.info("상품별 키워드 목록 조회 - productId: {}", productId);

        // 상품 존재 여부 확인
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다. productId: " + productId);
        }

        List<ProductSearchKeyword> keywords = keywordRepository.findByProductProductIdOrderByCreatedAtDesc(productId);

        log.info("키워드 목록 조회 완료 - productId: {}, 키워드 수: {}", productId, keywords.size());

        return keywords.stream()
                .map(SearchKeywordResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SearchKeywordResponse addKeyword(Long productId, SearchKeywordRequest request) {
        log.info("키워드 등록 - productId: {}, keyword: {}", productId, request.getKeyword());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. productId: " + productId));

        String keyword = request.getKeyword().trim();

        // 중복 체크
        if (keywordRepository.existsByProductProductIdAndKeyword(productId, keyword)) {
            throw new IllegalArgumentException("이미 등록된 키워드입니다. keyword: " + keyword);
        }

        ProductSearchKeyword searchKeyword = ProductSearchKeyword.builder()
                .product(product)
                .keyword(keyword)
                .build();

        ProductSearchKeyword savedKeyword = keywordRepository.save(searchKeyword);

        // 이벤트 발행을 위한 Outbox 저장
        saveKeywordCreatedEvent(savedKeyword);

        log.info("키워드 등록 완료 - keywordId: {}, productId: {}, keyword: {}",
                savedKeyword.getKeywordId(), productId, keyword);

        return SearchKeywordResponse.from(savedKeyword);
    }

    @Override
    @Transactional
    public void deleteKeyword(Long productId, Long keywordId) {
        log.info("키워드 삭제 - productId: {}, keywordId: {}", productId, keywordId);

        ProductSearchKeyword keyword = keywordRepository.findById(keywordId)
                .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다. keywordId: " + keywordId));

        // 상품 ID 일치 여부 확인
        if (!keyword.getProduct().getProductId().equals(productId)) {
            throw new IllegalArgumentException("해당 상품의 키워드가 아닙니다. productId: " + productId + ", keywordId: " + keywordId);
        }

        // 이벤트 발행을 위한 Outbox 저장 (삭제 전에 정보 저장)
        saveKeywordDeletedEvent(keyword);

        keywordRepository.delete(keyword);

        log.info("키워드 삭제 완료 - productId: {}, keywordId: {}", productId, keywordId);
    }

    private void saveKeywordCreatedEvent(ProductSearchKeyword keyword) {
        KeywordCreatedEvent event = KeywordCreatedEvent.builder()
                .keywordId(keyword.getKeywordId())
                .productId(keyword.getProduct().getProductId())
                .keyword(keyword.getKeyword())
                .createdAt(keyword.getCreatedAt())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType(AGGREGATE_TYPE_KEYWORD)
                    .aggregateId(String.valueOf(keyword.getKeywordId()))
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_CREATED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.debug("KeywordCreatedEvent Outbox 저장 완료 - keywordId: {}", keyword.getKeywordId());
        } catch (JsonProcessingException e) {
            log.error("KeywordCreatedEvent 직렬화 실패 - keywordId: {}", keyword.getKeywordId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    private void saveKeywordDeletedEvent(ProductSearchKeyword keyword) {
        KeywordDeletedEvent event = KeywordDeletedEvent.builder()
                .keywordId(keyword.getKeywordId())
                .productId(keyword.getProduct().getProductId())
                .keyword(keyword.getKeyword())
                .deletedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = Outbox.builder()
                    .aggregateType(AGGREGATE_TYPE_KEYWORD)
                    .aggregateId(String.valueOf(keyword.getKeywordId()))
                    .eventType(EventTypeConstants.TOPIC_KEYWORD_DELETED)
                    .payload(payload)
                    .build();

            outboxRepository.save(outbox);
            log.debug("KeywordDeletedEvent Outbox 저장 완료 - keywordId: {}", keyword.getKeywordId());
        } catch (JsonProcessingException e) {
            log.error("KeywordDeletedEvent 직렬화 실패 - keywordId: {}", keyword.getKeywordId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

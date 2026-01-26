package com.example.productservice.product.service;

import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductSearchKeyword;
import com.example.productservice.product.dto.SearchKeywordRequest;
import com.example.productservice.product.dto.SearchKeywordResponse;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductSearchKeywordServiceImpl implements ProductSearchKeywordService {

    private final ProductSearchKeywordRepository keywordRepository;
    private final ProductRepository productRepository;

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

        keywordRepository.delete(keyword);

        log.info("키워드 삭제 완료 - productId: {}, keywordId: {}", productId, keywordId);
    }
}

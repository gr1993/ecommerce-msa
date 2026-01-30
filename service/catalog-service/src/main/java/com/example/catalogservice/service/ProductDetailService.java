package com.example.catalogservice.service;

import com.example.catalogservice.client.ProductServiceClient;
import com.example.catalogservice.controller.dto.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDetailService {

    private static final String CACHE_KEY_PREFIX = "product:detail:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductServiceClient productServiceClient;

    /**
     * Data Enrichment 패턴: 이벤트 수신 시 product-service API를 호출하여
     * 상세 데이터를 가져온 뒤 Redis에 캐싱한다.
     */
    public void refreshCache(Long productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;

        log.info("Refreshing product detail cache: productId={}", productId);
        ProductDetailResponse response = productServiceClient.getProductDetail(productId);

        if (response != null) {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
            log.info("Product detail cache refreshed: productId={}", productId);
        }
    }

    public ProductDetailResponse getProductDetail(Long productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ProductDetailResponse response) {
            log.debug("Cache hit for product: {}", productId);
            return response;
        }

        log.debug("Cache miss for product: {}, fetching from product-service", productId);
        ProductDetailResponse response = productServiceClient.getProductDetail(productId);

        if (response != null) {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        }

        return response;
    }
}

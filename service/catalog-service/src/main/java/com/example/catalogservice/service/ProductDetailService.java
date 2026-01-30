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

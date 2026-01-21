package com.example.catalogservice.client;

import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.product.url}")
    private String productServiceUrl;

    public PageResponse<CatalogSyncProductResponse> getProductsForSync(int page, int size) {
        String url = UriComponentsBuilder.fromHttpUrl(productServiceUrl)
                .path("/api/internal/products/sync")
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        log.debug("Fetching products for sync: page={}, size={}, url={}", page, size, url);

        ResponseEntity<PageResponse<CatalogSyncProductResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }
}

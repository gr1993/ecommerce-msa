package com.example.catalogservice.client;

import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/internal/products/sync")
    PageResponse<CatalogSyncProductResponse> getProductsForSync(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}

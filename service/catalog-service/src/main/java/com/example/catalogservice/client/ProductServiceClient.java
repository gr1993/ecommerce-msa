package com.example.catalogservice.client;

import com.example.catalogservice.client.dto.CatalogSyncCategoryResponse;
import com.example.catalogservice.client.dto.CatalogSyncProductResponse;
import com.example.catalogservice.client.dto.PageResponse;
import com.example.catalogservice.controller.dto.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/api/internal/products/sync")
    PageResponse<CatalogSyncProductResponse> getProductsForSync(
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );

    @GetMapping("/api/internal/categories/sync")
    List<CatalogSyncCategoryResponse> getCategoriesForSync();

    @GetMapping("/api/admin/products/{productId}")
    ProductDetailResponse getProductDetail(@PathVariable("productId") Long productId);
}

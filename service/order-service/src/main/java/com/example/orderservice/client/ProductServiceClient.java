package com.example.orderservice.client;

import com.example.orderservice.client.dto.ProductDetailResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/api/admin/products/{productId}")
    ProductDetailResponse getProductDetail(@PathVariable("productId") Long productId);
}

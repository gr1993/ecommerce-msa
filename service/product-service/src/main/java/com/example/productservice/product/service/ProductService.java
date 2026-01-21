package com.example.productservice.product.service;

import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.product.dto.CatalogSyncProductResponse;
import com.example.productservice.product.dto.CatalogSyncRequest;
import com.example.productservice.product.dto.ProductCreateRequest;
import com.example.productservice.product.dto.ProductDetailResponse;
import com.example.productservice.product.dto.ProductResponse;
import com.example.productservice.product.dto.ProductSearchRequest;

public interface ProductService {

    PageResponse<ProductResponse> searchProducts(ProductSearchRequest request);

    ProductResponse createProduct(ProductCreateRequest request);

    ProductDetailResponse getProductDetail(Long productId);

    ProductResponse updateProduct(Long productId, ProductCreateRequest request);

    PageResponse<CatalogSyncProductResponse> getProductsForCatalogSync(CatalogSyncRequest request);
}

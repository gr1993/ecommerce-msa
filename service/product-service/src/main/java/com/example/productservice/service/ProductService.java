package com.example.productservice.service;

import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductCreateRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductSearchRequest;

public interface ProductService {

    PageResponse<ProductResponse> searchProducts(ProductSearchRequest request);

    ProductResponse createProduct(ProductCreateRequest request);
}

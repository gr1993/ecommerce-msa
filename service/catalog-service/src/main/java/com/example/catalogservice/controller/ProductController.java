package com.example.catalogservice.controller;

import com.example.catalogservice.controller.dto.ProductResponse;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product", description = "상품 조회 API")
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 조회합니다. 카테고리 ID로 필터링할 수 있습니다.")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @Parameter(description = "카테고리 ID (선택)") @RequestParam(value = "categoryId", required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductDocument> products = productSearchService.searchProducts(categoryId, pageable);
        Page<ProductResponse> response = products.map(ProductResponse::from);
        return ResponseEntity.ok(response);
    }
}

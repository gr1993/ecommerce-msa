package com.example.catalogservice.controller;

import com.example.catalogservice.controller.dto.PageResponse;
import com.example.catalogservice.controller.dto.ProductResponse;
import com.example.catalogservice.controller.dto.ProductSearchRequest;
import com.example.catalogservice.domain.document.ProductDocument;
import com.example.catalogservice.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Product", description = "상품 조회 API")
@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchService productSearchService;

    @Operation(summary = "상품명 자동완성", description = "입력된 키워드로 시작하는 상품명을 최대 5개까지 반환합니다.")
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocompleteProductName(
            @Parameter(description = "검색 키워드", required = true) @RequestParam("keyword") String keyword
    ) {
        List<String> suggestions = productSearchService.autocompleteProductName(keyword);
        return ResponseEntity.ok(suggestions);
    }

    @Operation(summary = "상품 목록 조회", description = "검색 필터와 페이지네이션을 적용하여 상품 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @Parameter(description = "상품명 (부분 검색)") @RequestParam(name = "productName", required = false) String productName,
            @Parameter(description = "카테고리 ID") @RequestParam(name = "categoryId", required = false) Long categoryId,
            @Parameter(description = "상품 상태 (ACTIVE, INACTIVE, SOLD_OUT)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "최소 가격") @RequestParam(name = "minPrice", required = false) Long minPrice,
            @Parameter(description = "최대 가격") @RequestParam(name = "maxPrice", required = false) Long maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @Parameter(description = "정렬 기준 (예: createdAt,desc)") @RequestParam(name = "sort", required = false) String sort
    ) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName(productName)
                .categoryId(categoryId)
                .status(status)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<ProductDocument> products = productSearchService.searchProducts(request);
        PageResponse<ProductResponse> response = PageResponse.from(products, ProductResponse::from);

        return ResponseEntity.ok(response);
    }
}

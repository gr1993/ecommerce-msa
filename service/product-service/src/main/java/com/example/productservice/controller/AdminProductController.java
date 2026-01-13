package com.example.productservice.controller;

import com.example.productservice.service.ProductService;
import com.example.productservice.dto.PageResponse;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Product", description = "관리자 상품 관리 API")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(
            summary = "상품 목록 조회",
            description = "페이지네이션과 검색 필터를 적용하여 상품 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @Parameter(description = "상품명 (부분 검색)") @RequestParam(name = "productName", required = false) String productName,
            @Parameter(description = "상품 코드") @RequestParam(name = "productCode", required = false) String productCode,
            @Parameter(description = "상품 상태 (ACTIVE, INACTIVE, SOLD_OUT)") @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "진열 여부") @RequestParam(name = "isDisplayed", required = false) Boolean isDisplayed,
            @Parameter(description = "최소 가격") @RequestParam(name = "minPrice", required = false) Double minPrice,
            @Parameter(description = "최대 가격") @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
            @Parameter(description = "정렬 기준 (예: createdAt,desc)") @RequestParam(name = "sort", required = false) String sort
    ) {
        log.info("GET /api/admin/products - productName: {}, productCode: {}, status: {}, isDisplayed: {}, page: {}, size: {}",
                productName, productCode, status, isDisplayed, page, size);

        ProductSearchRequest request = ProductSearchRequest.builder()
                .productName(productName)
                .productCode(productCode)
                .status(status)
                .isDisplayed(isDisplayed)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        PageResponse<ProductResponse> response = productService.searchProducts(request);

        return ResponseEntity.ok(response);
    }
}

package com.example.productservice.product.controller;

import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.product.dto.CatalogSyncProductResponse;
import com.example.productservice.product.dto.CatalogSyncRequest;
import com.example.productservice.product.service.ProductService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Product", description = "내부 서비스용 상품 API")
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/sync")
    @Operation(
            summary = "카탈로그 동기화용 상품 목록 조회",
            description = "Elasticsearch 동기화를 위한 상품 목록을 페이지네이션으로 조회합니다. ACTIVE 상태이고 진열 중인 상품만 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponseCatalogSyncProductResponse.class))
            )
    })
    public ResponseEntity<PageResponse<CatalogSyncProductResponse>> getProductsForSync(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(name = "size", required = false, defaultValue = "100") Integer size
    ) {
        log.info("GET /api/internal/products/sync - page: {}, size: {}", page, size);

        CatalogSyncRequest request = CatalogSyncRequest.builder()
                .page(page)
                .size(size)
                .build();

        PageResponse<CatalogSyncProductResponse> response = productService.getProductsForCatalogSync(request);

        return ResponseEntity.ok(response);
    }

    @Schema(name = "PageResponseCatalogSyncProductResponse", description = "카탈로그 동기화 상품 페이지 응답")
    private static class PageResponseCatalogSyncProductResponse extends PageResponse<CatalogSyncProductResponse> {
    }
}

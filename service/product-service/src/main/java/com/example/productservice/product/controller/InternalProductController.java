package com.example.productservice.product.controller;

import com.example.productservice.global.common.dto.PageResponse;
import com.example.productservice.product.dto.CatalogSyncProductResponse;
import com.example.productservice.product.dto.CatalogSyncRequest;
import com.example.productservice.product.dto.ProductResponse;
import com.example.productservice.product.service.ProductService;
import com.example.productservice.product.service.SampleDataService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Product", description = "내부 서비스용 상품 API")
public class InternalProductController {

    private final ProductService productService;
    private final SampleDataService sampleDataService;

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

    @DeleteMapping
    @Operation(
            summary = "전체 상품 데이터 삭제",
            description = "모든 상품 및 관련 데이터(product, product_category, product_search_keyword, outbox, file_upload)를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    public ResponseEntity<Void> deleteAllProducts() {
        log.info("DELETE /api/internal/products - Deleting all products");

        productService.deleteAllProducts();

        log.info("All products and related data deleted successfully");

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sample-data")
    @Operation(
            summary = "샘플 상품 데이터 생성",
            description = "테스트용 샘플 상품 15개를 생성합니다. 자동완성 테스트를 위해 비슷한 이름의 상품들이 포함됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "샘플 상품 생성 성공"
            )
    })
    public ResponseEntity<List<ProductResponse>> createSampleProducts() {
        log.info("POST /api/internal/products/sample-data - Creating sample products");

        List<ProductResponse> createdProducts = sampleDataService.createSampleProducts();

        log.info("Created {} sample products", createdProducts.size());

        return ResponseEntity.ok(createdProducts);
    }
}

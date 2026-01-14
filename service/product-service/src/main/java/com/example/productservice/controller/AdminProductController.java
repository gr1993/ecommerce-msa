package com.example.productservice.controller;

import com.example.productservice.service.FileStorageService;
import com.example.productservice.service.ProductService;
import com.example.productservice.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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
    private final FileStorageService fileStorageService;

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

    @GetMapping("/{productId}")
    @Operation(
            summary = "상품 상세 조회",
            description = "상품 ID로 상품의 상세 정보를 조회합니다. 옵션 그룹, SKU, 이미지 정보를 포함합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<ProductDetailResponse> getProductDetail(
            @Parameter(description = "상품 ID") @PathVariable("productId") Long productId
    ) {
        log.info("GET /api/admin/products/{} - 상품 상세 조회", productId);

        try {
            ProductDetailResponse response = productService.getProductDetail(productId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("상품을 찾을 수 없습니다. productId: {}", productId);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(
            summary = "상품 등록",
            description = "옵션, SKU, 이미지를 포함한 상품을 등록합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        log.info("POST /api/admin/products - productName: {}", request.getProductName());

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.status(201).body(response);
    }

    @PostMapping(
            value = "/files/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "파일 업로드",
            description = "상품 이미지를 임시 저장합니다. 상품 등록 시 확정됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업로드 성공",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "업로드할 파일") @RequestParam(name = "file") MultipartFile file
    ) {
        log.info("POST /api/admin/products/files/upload - filename: {}", file.getOriginalFilename());

        try {
            FileUploadResponse response = fileStorageService.uploadFile(file);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

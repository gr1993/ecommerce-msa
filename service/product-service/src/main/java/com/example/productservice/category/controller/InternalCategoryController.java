package com.example.productservice.category.controller;

import com.example.productservice.category.dto.CatalogSyncCategoryResponse;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.service.CategoryService;
import com.example.productservice.category.service.SampleCategoryDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Category", description = "내부 서비스용 카테고리 API")
public class InternalCategoryController {

    private final CategoryService categoryService;
    private final SampleCategoryDataService sampleCategoryDataService;

    @GetMapping("/sync")
    @Operation(
            summary = "카탈로그 동기화용 카테고리 목록 조회",
            description = "Elasticsearch 동기화를 위한 카테고리 목록을 조회합니다. 계층 구조가 아닌 평탄화된 리스트로 반환하며, depth 필드로 계층 레벨을 표현합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CatalogSyncCategoryResponse.class)))
            )
    })
    public ResponseEntity<List<CatalogSyncCategoryResponse>> getCategoriesForSync() {
        log.info("GET /api/internal/categories/sync");

        List<CatalogSyncCategoryResponse> response = categoryService.getCategoriesForCatalogSync();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sample-data")
    @Operation(
            summary = "샘플 카테고리 데이터 생성",
            description = "테스트용 샘플 카테고리를 생성합니다. 카테고리 ID가 이미 존재하면 업데이트하고, 없으면 지정된 ID로 생성합니다 (upsert)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "샘플 카테고리 생성/업데이트 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)))
            )
    })
    public ResponseEntity<List<CategoryResponse>> createSampleCategories() {
        log.info("POST /api/internal/categories/sample-data - Creating sample categories");

        List<CategoryResponse> createdCategories = sampleCategoryDataService.createSampleCategories();

        log.info("Upserted {} sample categories", createdCategories.size());

        return ResponseEntity.ok(createdCategories);
    }
}

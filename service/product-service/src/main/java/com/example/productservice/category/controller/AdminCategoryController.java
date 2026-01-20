package com.example.productservice.category.controller;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.dto.CategoryTreeResponse;
import com.example.productservice.category.dto.CategoryUpdateRequest;
import com.example.productservice.category.service.CategoryService;

import java.util.List;

import com.example.productservice.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Category", description = "관리자 카테고리 관리 API")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
            summary = "카테고리 트리 조회",
            description = "모든 카테고리를 계층 구조(트리)로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryTreeResponse.class))
            )
    })
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        log.info("GET /api/admin/categories");

        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(tree);
    }

    @GetMapping("/{categoryId}")
    @Operation(
            summary = "카테고리 상세 조회",
            description = "특정 카테고리의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable(name = "categoryId") Long categoryId) {
        log.info("GET /api/admin/categories/{}", categoryId);

        try {
            CategoryResponse response = categoryService.getCategory(categoryId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("카테고리 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(
            summary = "카테고리 등록",
            description = "새로운 카테고리를 등록합니다. 상위 카테고리 ID를 지정하여 계층 구조를 만들 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상위 카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        log.info("POST /api/admin/categories - categoryName: {}, parentId: {}",
                request.getCategoryName(), request.getParentId());

        try {
            CategoryResponse response = categoryService.createCategory(request);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("카테고리 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{categoryId}")
    @Operation(
            summary = "카테고리 수정",
            description = "특정 카테고리의 정보를 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable(name = "categoryId") Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        log.info("PUT /api/admin/categories/{} - categoryName: {}", categoryId, request.getCategoryName());

        try {
            CategoryResponse response = categoryService.updateCategory(categoryId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("카테고리 수정 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{categoryId}")
    @Operation(
            summary = "카테고리 삭제",
            description = "특정 카테고리를 삭제합니다. 하위 카테고리가 존재하면 삭제할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "하위 카테고리가 존재하여 삭제 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteCategory(@PathVariable(name = "categoryId") Long categoryId) {
        log.info("DELETE /api/admin/categories/{}", categoryId);

        try {
            categoryService.deleteCategory(categoryId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("카테고리 삭제 실패 - 카테고리 없음: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("카테고리 삭제 실패 - 하위 카테고리 존재: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

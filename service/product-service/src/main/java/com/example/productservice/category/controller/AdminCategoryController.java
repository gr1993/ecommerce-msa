package com.example.productservice.category.controller;

import com.example.productservice.category.dto.CategoryCreateRequest;
import com.example.productservice.category.dto.CategoryResponse;
import com.example.productservice.category.service.CategoryService;
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
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "상위 카테고리를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = String.class))
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
}

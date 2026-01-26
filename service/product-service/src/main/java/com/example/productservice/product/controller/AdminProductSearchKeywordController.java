package com.example.productservice.product.controller;

import com.example.productservice.global.exception.ErrorResponse;
import com.example.productservice.product.dto.SearchKeywordRequest;
import com.example.productservice.product.dto.SearchKeywordResponse;
import com.example.productservice.product.service.ProductSearchKeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/products/{productId}/keywords")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Product Search Keyword", description = "관리자 상품 검색 키워드 관리 API")
public class AdminProductSearchKeywordController {

    private final ProductSearchKeywordService keywordService;

    @GetMapping
    @Operation(
            summary = "상품별 검색 키워드 목록 조회",
            description = "특정 상품에 등록된 검색 키워드 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SearchKeywordResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (상품이 존재하지 않음)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<SearchKeywordResponse>> getKeywords(
            @Parameter(description = "상품 ID", required = true) @PathVariable("productId") Long productId
    ) {
        log.info("GET /api/admin/products/{}/keywords", productId);

        List<SearchKeywordResponse> keywords = keywordService.getKeywordsByProductId(productId);

        return ResponseEntity.ok(keywords);
    }

    @PostMapping
    @Operation(
            summary = "검색 키워드 등록",
            description = "특정 상품에 검색 키워드를 등록합니다. 동일 상품에 중복 키워드는 등록할 수 없습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "등록 성공",
                    content = @Content(schema = @Schema(implementation = SearchKeywordResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (상품 미존재, 중복 키워드, 유효성 검증 실패)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<SearchKeywordResponse> addKeyword(
            @Parameter(description = "상품 ID", required = true) @PathVariable("productId") Long productId,
            @Valid @RequestBody SearchKeywordRequest request
    ) {
        log.info("POST /api/admin/products/{}/keywords - keyword: {}", productId, request.getKeyword());

        SearchKeywordResponse response = keywordService.addKeyword(productId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{keywordId}")
    @Operation(
            summary = "검색 키워드 삭제",
            description = "특정 상품의 검색 키워드를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (키워드 미존재, 상품 불일치)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteKeyword(
            @Parameter(description = "상품 ID", required = true) @PathVariable("productId") Long productId,
            @Parameter(description = "키워드 ID", required = true) @PathVariable("keywordId") Long keywordId
    ) {
        log.info("DELETE /api/admin/products/{}/keywords/{}", productId, keywordId);

        keywordService.deleteKeyword(productId, keywordId);

        return ResponseEntity.noContent().build();
    }
}

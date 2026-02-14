package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.AdminDiscountPolicyCreateRequest;
import com.example.promotionservice.dto.request.AdminDiscountPolicyUpdateRequest;
import com.example.promotionservice.dto.response.AdminDiscountPolicyDetailResponse;
import com.example.promotionservice.dto.response.AdminDiscountPolicyResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import com.example.promotionservice.global.exception.ErrorResponse;
import com.example.promotionservice.service.AdminDiscountPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Discount Policy", description = "관리자 할인 정책 관리 API")
@RestController
@RequestMapping("/api/admin/discount-policies")
@RequiredArgsConstructor
public class AdminDiscountPolicyController {

    private final AdminDiscountPolicyService adminDiscountPolicyService;

    @Operation(
            summary = "할인 정책 목록 조회",
            description = "관리자용 할인 정책 목록을 조회합니다. 정책명 및 상태로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminDiscountPolicyResponse>> getDiscountPolicies(
            @Parameter(description = "할인 정책명 (부분 검색)", example = "여름")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "할인 상태", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminDiscountPolicyResponse> policies = adminDiscountPolicyService.getDiscountPolicies(keyword, status, pageable);
        return ResponseEntity.ok(policies);
    }

    @Operation(
            summary = "할인 정책 상세 조회",
            description = "관리자용 할인 정책 상세 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = AdminDiscountPolicyDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "할인 정책을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/{discountId}")
    public ResponseEntity<AdminDiscountPolicyDetailResponse> getDiscountPolicyDetail(
            @Parameter(description = "할인 정책 ID", required = true, example = "1")
            @PathVariable Long discountId) {
        AdminDiscountPolicyDetailResponse policy = adminDiscountPolicyService.getDiscountPolicyDetail(discountId);
        return ResponseEntity.ok(policy);
    }

    @Operation(
            summary = "할인 정책 등록",
            description = "관리자가 새 할인 정책을 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "등록 성공",
                            content = @Content(schema = @Schema(implementation = AdminDiscountPolicyDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping
    public ResponseEntity<AdminDiscountPolicyDetailResponse> createDiscountPolicy(
            @Valid @RequestBody AdminDiscountPolicyCreateRequest request) {
        AdminDiscountPolicyDetailResponse policy = adminDiscountPolicyService.createDiscountPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(policy);
    }

    @Operation(
            summary = "할인 정책 수정",
            description = "관리자가 할인 정책 정보를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = AdminDiscountPolicyDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "할인 정책을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PutMapping("/{discountId}")
    public ResponseEntity<AdminDiscountPolicyDetailResponse> updateDiscountPolicy(
            @Parameter(description = "할인 정책 ID", required = true, example = "1")
            @PathVariable Long discountId,
            @Valid @RequestBody AdminDiscountPolicyUpdateRequest request) {
        AdminDiscountPolicyDetailResponse policy = adminDiscountPolicyService.updateDiscountPolicy(discountId, request);
        return ResponseEntity.ok(policy);
    }
}

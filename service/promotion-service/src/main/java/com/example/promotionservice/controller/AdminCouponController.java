package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.AdminCouponCreateRequest;
import com.example.promotionservice.dto.request.AdminCouponUpdateRequest;
import com.example.promotionservice.dto.response.AdminCouponDetailResponse;
import com.example.promotionservice.dto.response.AdminCouponResponse;
import com.example.promotionservice.global.common.dto.PageResponse;
import com.example.promotionservice.global.exception.ErrorResponse;
import com.example.promotionservice.service.AdminCouponService;
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

@Tag(name = "Admin Coupon", description = "관리자 쿠폰 관리 API")
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @Operation(
            summary = "쿠폰 목록 조회",
            description = "관리자용 쿠폰 목록을 조회합니다. 쿠폰 코드/쿠폰명 및 상태로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminCouponResponse>> getCoupons(
            @Parameter(description = "쿠폰 코드 또는 쿠폰명 (부분 검색)", example = "WELCOME")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "쿠폰 상태", example = "ACTIVE")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminCouponResponse> coupons = adminCouponService.getCoupons(keyword, status, pageable);
        return ResponseEntity.ok(coupons);
    }

    @Operation(
            summary = "쿠폰 상세 조회",
            description = "관리자용 쿠폰 상세 정보를 조회합니다. 쿠폰 발급 내역을 포함합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = AdminCouponDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "쿠폰을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/{couponId}")
    public ResponseEntity<AdminCouponDetailResponse> getCouponDetail(
            @Parameter(description = "쿠폰 ID", required = true, example = "1")
            @PathVariable Long couponId) {
        AdminCouponDetailResponse coupon = adminCouponService.getCouponDetail(couponId);
        return ResponseEntity.ok(coupon);
    }

    @Operation(
            summary = "쿠폰 등록",
            description = "관리자가 새 쿠폰을 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "등록 성공",
                            content = @Content(schema = @Schema(implementation = AdminCouponDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping
    public ResponseEntity<AdminCouponDetailResponse> createCoupon(
            @Valid @RequestBody AdminCouponCreateRequest request) {
        AdminCouponDetailResponse coupon = adminCouponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @Operation(
            summary = "쿠폰 수정",
            description = "관리자가 쿠폰 정보를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = AdminCouponDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "쿠폰을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PutMapping("/{couponId}")
    public ResponseEntity<AdminCouponDetailResponse> updateCoupon(
            @Parameter(description = "쿠폰 ID", required = true, example = "1")
            @PathVariable Long couponId,
            @Valid @RequestBody AdminCouponUpdateRequest request) {
        AdminCouponDetailResponse coupon = adminCouponService.updateCoupon(couponId, request);
        return ResponseEntity.ok(coupon);
    }
}

package com.example.promotionservice.controller;

import com.example.promotionservice.dto.request.CouponClaimRequest;
import com.example.promotionservice.dto.response.ApplicableDiscountPolicyResponse;
import com.example.promotionservice.dto.response.CouponClaimResponse;
import com.example.promotionservice.dto.response.UserCouponResponse;
import com.example.promotionservice.global.exception.ErrorResponse;
import com.example.promotionservice.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Promotion", description = "사용자 프로모션 관리")
@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(
            summary = "내 쿠폰 목록 조회",
            description = "사용자가 보유한 쿠폰 목록을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "쿠폰 목록 조회 성공",
                            content = @Content(schema = @Schema(implementation = UserCouponResponse.class))
                    )
            }
    )
    @GetMapping("/coupons")
    public ResponseEntity<List<UserCouponResponse>> getUserCoupons(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<UserCouponResponse> response = promotionService.getUserCoupons(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "쿠폰 등록",
            description = "쿠폰 코드로 사용자에게 쿠폰을 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "쿠폰 등록 성공",
                            content = @Content(schema = @Schema(implementation = CouponClaimResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (유효하지 않은 쿠폰, 이미 등록된 쿠폰 등)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "쿠폰을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/coupon")
    public ResponseEntity<CouponClaimResponse> claimCoupon(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CouponClaimRequest request
    ) {
        CouponClaimResponse response = promotionService.claimCoupon(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "적용 가능 할인 정책 조회",
            description = "주어진 상품 ID 목록에 적용 가능한 할인 정책(ACTIVE 상태, 유효기간 내)을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "할인 정책 조회 성공",
                            content = @Content(schema = @Schema(implementation = ApplicableDiscountPolicyResponse.class))
                    )
            }
    )
    @GetMapping("/discount-policies")
    public ResponseEntity<List<ApplicableDiscountPolicyResponse>> getApplicableDiscountPolicies(
            @Parameter(description = "상품 ID 목록", required = true)
            @RequestParam List<Long> productIds
    ) {
        List<ApplicableDiscountPolicyResponse> response = promotionService.getApplicableDiscountPolicies(productIds);
        return ResponseEntity.ok(response);
    }
}

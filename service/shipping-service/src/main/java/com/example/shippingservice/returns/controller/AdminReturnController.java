package com.example.shippingservice.returns.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.returns.dto.request.AdminReturnApproveRequest;
import com.example.shippingservice.returns.dto.request.AdminReturnRejectRequest;
import com.example.shippingservice.returns.dto.response.AdminReturnResponse;
import com.example.shippingservice.returns.service.AdminReturnService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Return", description = "관리자 반품 관리 API")
@RestController
@RequestMapping("/api/admin/shipping/returns")
@RequiredArgsConstructor
public class AdminReturnController {

    private final AdminReturnService adminReturnService;

    @Operation(
            summary = "반품 목록 조회",
            description = "관리자용 반품 목록을 조회합니다. 반품 상태 및 주문 ID로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminReturnResponse>> getReturns(
            @Parameter(description = "반품 상태 (RETURN_REQUESTED, RETURN_APPROVED, RETURN_REJECTED, RETURNED)", example = "RETURN_REQUESTED")
            @RequestParam(required = false) String returnStatus,
            @Parameter(description = "주문 ID", example = "1")
            @RequestParam(required = false) Long orderId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminReturnResponse> response = adminReturnService.getReturns(returnStatus, orderId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "반품 상세 조회",
            description = "반품 ID로 반품 상세 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "반품 정보 없음")
            }
    )
    @GetMapping("/{returnId}")
    public ResponseEntity<AdminReturnResponse> getReturn(
            @Parameter(description = "반품 ID", required = true, example = "1")
            @PathVariable Long returnId) {
        AdminReturnResponse response = adminReturnService.getReturn(returnId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "반품 승인",
            description = "반품을 승인하고 수거지(창고) 정보를 설정합니다. RETURN_REQUESTED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "승인 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "409", description = "승인 불가 상태")
            }
    )
    @PatchMapping("/{returnId}/approve")
    public ResponseEntity<AdminReturnResponse> approveReturn(
            @Parameter(description = "반품 ID", required = true, example = "1")
            @PathVariable Long returnId,
            @Valid @RequestBody AdminReturnApproveRequest request) {
        AdminReturnResponse response = adminReturnService.approveReturn(returnId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "반품 거절",
            description = "반품을 거절합니다. RETURN_REQUESTED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "거절 성공"),
                    @ApiResponse(responseCode = "409", description = "거절 불가 상태")
            }
    )
    @PatchMapping("/{returnId}/reject")
    public ResponseEntity<AdminReturnResponse> rejectReturn(
            @Parameter(description = "반품 ID", required = true, example = "1")
            @PathVariable Long returnId,
            @RequestBody(required = false) AdminReturnRejectRequest request) {
        if (request == null) {
            request = new AdminReturnRejectRequest();
        }
        AdminReturnResponse response = adminReturnService.rejectReturn(returnId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "반품 완료 처리",
            description = "반품을 완료 처리합니다. order_shipping 상태도 RETURNED로 변경됩니다. RETURN_APPROVED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "완료 처리 성공"),
                    @ApiResponse(responseCode = "409", description = "완료 불가 상태")
            }
    )
    @PatchMapping("/{returnId}/complete")
    public ResponseEntity<AdminReturnResponse> completeReturn(
            @Parameter(description = "반품 ID", required = true, example = "1")
            @PathVariable Long returnId) {
        AdminReturnResponse response = adminReturnService.completeReturn(returnId);
        return ResponseEntity.ok(response);
    }
}

package com.example.shippingservice.exchange.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.request.AdminExchangeApproveRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeRejectRequest;
import com.example.shippingservice.exchange.dto.response.AdminExchangeResponse;
import com.example.shippingservice.exchange.service.AdminExchangeService;
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

@Tag(name = "Admin Exchange", description = "관리자 교환 관리 API")
@RestController
@RequestMapping("/api/admin/shipping/exchanges")
@RequiredArgsConstructor
public class AdminExchangeController {

    private final AdminExchangeService adminExchangeService;

    @Operation(
            summary = "교환 목록 조회",
            description = "관리자용 교환 목록을 조회합니다. 교환 상태 및 주문 ID로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminExchangeResponse>> getExchanges(
            @Parameter(description = "교환 상태 (EXCHANGE_REQUESTED, EXCHANGE_APPROVED, EXCHANGE_REJECTED, EXCHANGED)", example = "EXCHANGE_REQUESTED")
            @RequestParam(required = false) String exchangeStatus,
            @Parameter(description = "주문 ID", example = "1")
            @RequestParam(required = false) Long orderId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminExchangeResponse> response = adminExchangeService.getExchanges(exchangeStatus, orderId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "교환 상세 조회",
            description = "교환 ID로 교환 상세 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "교환 정보 없음")
            }
    )
    @GetMapping("/{exchangeId}")
    public ResponseEntity<AdminExchangeResponse> getExchange(
            @Parameter(description = "교환 ID", required = true, example = "1")
            @PathVariable Long exchangeId) {
        AdminExchangeResponse response = adminExchangeService.getExchange(exchangeId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "교환 승인",
            description = "교환을 승인하고 교환품 배송지를 설정합니다. Mock 택배사 API를 통해 교환품 송장이 자동 발급됩니다. EXCHANGE_REQUESTED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "승인 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "409", description = "승인 불가 상태")
            }
    )
    @PatchMapping("/{exchangeId}/approve")
    public ResponseEntity<AdminExchangeResponse> approveExchange(
            @Parameter(description = "교환 ID", required = true, example = "1")
            @PathVariable Long exchangeId,
            @Valid @RequestBody AdminExchangeApproveRequest request) {
        AdminExchangeResponse response = adminExchangeService.approveExchange(exchangeId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "교환 거절",
            description = "교환을 거절합니다. EXCHANGE_REQUESTED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "거절 성공"),
                    @ApiResponse(responseCode = "409", description = "거절 불가 상태")
            }
    )
    @PatchMapping("/{exchangeId}/reject")
    public ResponseEntity<AdminExchangeResponse> rejectExchange(
            @Parameter(description = "교환 ID", required = true, example = "1")
            @PathVariable Long exchangeId,
            @RequestBody(required = false) AdminExchangeRejectRequest request) {
        if (request == null) {
            request = new AdminExchangeRejectRequest();
        }
        AdminExchangeResponse response = adminExchangeService.rejectExchange(exchangeId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "교환 완료 처리",
            description = "교환을 완료 처리합니다. EXCHANGE_APPROVED 상태에서만 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "완료 처리 성공"),
                    @ApiResponse(responseCode = "409", description = "완료 불가 상태")
            }
    )
    @PatchMapping("/{exchangeId}/complete")
    public ResponseEntity<AdminExchangeResponse> completeExchange(
            @Parameter(description = "교환 ID", required = true, example = "1")
            @PathVariable Long exchangeId) {
        AdminExchangeResponse response = adminExchangeService.completeExchange(exchangeId);
        return ResponseEntity.ok(response);
    }
}

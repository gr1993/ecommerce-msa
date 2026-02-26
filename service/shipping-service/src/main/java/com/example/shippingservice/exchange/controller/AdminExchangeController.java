package com.example.shippingservice.exchange.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.exchange.dto.request.AdminExchangeApproveRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeRejectRequest;
import com.example.shippingservice.exchange.dto.request.AdminExchangeShippingRequest;
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

    @Operation(summary = "교환 목록 조회", description = "교환 상태 및 주문 ID로 필터링",
            responses = @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))))
    @GetMapping
    public ResponseEntity<PageResponse<AdminExchangeResponse>> getExchanges(
            @Parameter(description = "교환 상태") @RequestParam(required = false) String exchangeStatus,
            @Parameter(description = "주문 ID") @RequestParam(required = false) Long orderId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminExchangeService.getExchanges(exchangeStatus, orderId, pageable));
    }

    @Operation(summary = "교환 상세 조회",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    @GetMapping("/{exchangeId}")
    public ResponseEntity<AdminExchangeResponse> getExchange(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId) {
        return ResponseEntity.ok(adminExchangeService.getExchange(exchangeId));
    }

    @Operation(summary = "교환 승인",
            description = "회수 수거지 정보를 입력하고 Mock 택배사로 회수 운송장을 자동 발급합니다. " +
                    "EXCHANGE_REQUESTED → EXCHANGE_COLLECTING",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "409")})
    @PatchMapping("/{exchangeId}/approve")
    public ResponseEntity<AdminExchangeResponse> approveExchange(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId,
            @Valid @RequestBody AdminExchangeApproveRequest request) {
        return ResponseEntity.ok(adminExchangeService.approveExchange(exchangeId, request));
    }

    @Operation(summary = "교환 거절",
            description = "EXCHANGE_REQUESTED → EXCHANGE_REJECTED",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "409")})
    @PatchMapping("/{exchangeId}/reject")
    public ResponseEntity<AdminExchangeResponse> rejectExchange(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId,
            @RequestBody(required = false) AdminExchangeRejectRequest request) {
        if (request == null) {
            request = new AdminExchangeRejectRequest();
        }
        return ResponseEntity.ok(adminExchangeService.rejectExchange(exchangeId, request));
    }

    @Operation(summary = "회수 완료 처리",
            description = "EXCHANGE_COLLECTING → EXCHANGE_RETURN_COMPLETED",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "409")})
    @PatchMapping("/{exchangeId}/collect-complete")
    public ResponseEntity<AdminExchangeResponse> completeCollect(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId) {
        return ResponseEntity.ok(adminExchangeService.completeCollect(exchangeId));
    }

    @Operation(summary = "교환 배송 시작",
            description = "교환품 배송지를 입력하고 Mock 택배사로 배송 운송장을 자동 발급합니다. " +
                    "EXCHANGE_RETURN_COMPLETED → EXCHANGE_SHIPPING",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "409")})
    @PatchMapping("/{exchangeId}/shipping")
    public ResponseEntity<AdminExchangeResponse> startShipping(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId,
            @Valid @RequestBody AdminExchangeShippingRequest request) {
        return ResponseEntity.ok(adminExchangeService.startShipping(exchangeId, request));
    }

    @Operation(summary = "교환 완료 처리",
            description = "EXCHANGE_SHIPPING → EXCHANGED",
            responses = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "409")})
    @PatchMapping("/{exchangeId}/complete")
    public ResponseEntity<AdminExchangeResponse> completeExchange(
            @Parameter(description = "교환 ID", required = true) @PathVariable Long exchangeId) {
        return ResponseEntity.ok(adminExchangeService.completeExchange(exchangeId));
    }
}

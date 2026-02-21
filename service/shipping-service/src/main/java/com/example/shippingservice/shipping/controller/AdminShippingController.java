package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.request.RegisterTrackingRequest;
import com.example.shippingservice.shipping.dto.response.AdminShippingPageResponse;
import com.example.shippingservice.shipping.dto.response.AdminShippingResponse;
import com.example.shippingservice.shipping.service.AdminShippingService;
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

@Tag(name = "Admin Shipping", description = "관리자 배송 관리 API")
@RestController
@RequestMapping("/api/admin/shipping")
@RequiredArgsConstructor
public class AdminShippingController {

    private final AdminShippingService adminShippingService;

    @Operation(
            summary = "배송 목록 조회",
            description = "관리자용 배송 목록을 조회합니다. 배송 상태 및 운송장 번호로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = AdminShippingPageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminShippingResponse>> getShippings(
            @Parameter(description = "배송 상태 (READY, SHIPPING, DELIVERED, RETURNED)", example = "READY")
            @RequestParam(required = false) String shippingStatus,
            @Parameter(description = "운송장 번호 (부분 검색)", example = "123456")
            @RequestParam(required = false) String trackingNumber,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminShippingResponse> shippings = adminShippingService.getShippings(shippingStatus, trackingNumber, pageable);
        return ResponseEntity.ok(shippings);
    }

    @Operation(
            summary = "운송장 등록",
            description = "택배사 API를 통해 운송장을 발급하고 배송 정보를 업데이트합니다. deliveryServiceStatus가 NOT_SENT인 경우에만 허용됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "운송장 등록 성공",
                            content = @Content(schema = @Schema(implementation = AdminShippingResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 택배사 코드 등)"),
                    @ApiResponse(responseCode = "404", description = "배송 정보 없음"),
                    @ApiResponse(responseCode = "409", description = "이미 택배사에 전송된 배송 건")
            }
    )
    @PostMapping("/{shippingId}/tracking")
    public ResponseEntity<AdminShippingResponse> registerTracking(
            @Parameter(description = "배송 ID", example = "1")
            @PathVariable Long shippingId,
            @Valid @RequestBody RegisterTrackingRequest request) {
        AdminShippingResponse response = adminShippingService.registerTracking(shippingId, request);
        return ResponseEntity.ok(response);
    }
}

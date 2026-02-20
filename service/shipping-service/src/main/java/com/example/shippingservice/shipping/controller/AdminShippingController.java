package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.AdminShippingPageResponse;
import com.example.shippingservice.shipping.dto.response.AdminShippingResponse;
import com.example.shippingservice.shipping.service.AdminShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}

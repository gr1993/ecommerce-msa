package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.client.dto.PageResponse;
import com.example.shippingservice.shipping.dto.response.MarketShippingResponse;
import com.example.shippingservice.shipping.service.MarketShippingService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Market Shipping", description = "사용자 배송 조회 API")
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class MarketShippingController {

    private final MarketShippingService marketShippingService;

    @Operation(
            summary = "내 배송 목록 조회",
            description = "로그인한 사용자의 배송 목록을 최신순으로 조회합니다. API Gateway에서 전달하는 X-User-Id 헤더로 사용자를 식별합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "X-User-Id 헤더 누락")
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<MarketShippingResponse>> getMyShippings(
            @Parameter(description = "API Gateway가 JWT 검증 후 주입하는 사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MarketShippingResponse> response = marketShippingService.getMyShippings(userId, pageable);
        return ResponseEntity.ok(response);
    }
}

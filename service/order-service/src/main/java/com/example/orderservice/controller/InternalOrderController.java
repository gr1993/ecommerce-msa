package com.example.orderservice.controller;

import com.example.orderservice.dto.request.ShippingSyncRequest;
import com.example.orderservice.dto.response.ShippingSyncOrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Order", description = "내부 서비스용 주문 API")
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/sync")
    @Operation(
            summary = "배송 동기화용 주문 목록 조회",
            description = "배송 서비스 동기화를 위한 주문 목록을 페이지네이션으로 조회합니다. PAID 상태의 주문만 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PageResponseShippingSyncOrderResponse.class))
            )
    })
    public ResponseEntity<PageResponse<ShippingSyncOrderResponse>> getOrdersForSync(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @Parameter(description = "페이지 크기") @RequestParam(name = "size", required = false, defaultValue = "100") Integer size
    ) {
        log.info("GET /api/internal/orders/sync - page: {}, size: {}", page, size);

        ShippingSyncRequest request = ShippingSyncRequest.builder()
                .page(page)
                .size(size)
                .build();

        PageResponse<ShippingSyncOrderResponse> response = orderService.getOrdersForShippingSync(request);

        return ResponseEntity.ok(response);
    }

    @Schema(name = "PageResponseShippingSyncOrderResponse", description = "배송 동기화 주문 페이지 응답")
    private static class PageResponseShippingSyncOrderResponse extends PageResponse<ShippingSyncOrderResponse> {
    }
}

package com.example.orderservice.controller;

import com.example.orderservice.dto.request.ShippingSyncRequest;
import com.example.orderservice.dto.request.TestCreateOrderRequest;
import com.example.orderservice.dto.response.ShippingSyncOrderResponse;
import com.example.orderservice.dto.response.TestCreateOrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.service.InternalOrderService;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Order", description = "내부 서비스용 주문 API")
public class InternalOrderController {

    private final OrderService orderService;
    private final InternalOrderService internalOrderService;

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

    @PostMapping("/test/orders")
    @Operation(
            summary = "[테스트] 결제 완료 주문 생성",
            description = "테스트용 결제 완료 주문 생성 API입니다. PAID 상태의 주문을 생성하고, "
                    + "shipping-service와 payment-service에도 테스트 데이터를 함께 생성합니다. "
                    + "동일한 주문번호로 재요청해도 기존 데이터를 반환하여 멱등성을 보장합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 생성 성공")
    })
    public ResponseEntity<TestCreateOrderResponse> createOrderForTest(
            @Valid @RequestBody TestCreateOrderRequest request) {
        log.info("[TEST] POST /api/internal/orders/test/orders - userId: {}", request.getUserId());
        TestCreateOrderResponse response = internalOrderService.createOrderForTest(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/test/orders/{orderId}")
    @Operation(
            summary = "[테스트] 주문 및 관련 데이터 삭제",
            description = "테스트 데이터 정리를 위한 삭제 API입니다. "
                    + "주문 ID에 해당하는 주문 데이터를 삭제하고, shipping-service와 payment-service의 "
                    + "관련 테스트 데이터도 함께 삭제합니다. 해당 데이터가 없어도 오류를 발생시키지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    public ResponseEntity<Void> deleteOrderForTest(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId) {
        log.info("[TEST] DELETE /api/internal/orders/test/orders/{}", orderId);
        internalOrderService.deleteOrderForTest(orderId);
        return ResponseEntity.noContent().build();
    }
}

package com.example.orderservice.controller;

import com.example.orderservice.dto.request.AdminOrderUpdateRequest;
import com.example.orderservice.dto.request.CancelOrderRequest;
import com.example.orderservice.dto.response.AdminOrderDetailResponse;
import com.example.orderservice.dto.response.AdminOrderResponse;
import com.example.orderservice.dto.response.CancelOrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.global.exception.ErrorResponse;
import com.example.orderservice.service.AdminOrderService;
import com.example.orderservice.service.OrderCancellationService;
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

@Tag(name = "Admin Order", description = "관리자 주문 관리 API")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final OrderCancellationService orderCancellationService;

    @Operation(
            summary = "주문 목록 조회",
            description = "관리자용 주문 목록을 조회합니다. 주문 번호 및 주문 상태로 필터링할 수 있습니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<AdminOrderResponse>> getOrders(
            @Parameter(description = "주문 번호 (부분 검색)", example = "ORD-2024")
            @RequestParam(required = false) String orderNumber,
            @Parameter(description = "주문 상태", example = "PAID")
            @RequestParam(required = false) String orderStatus,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminOrderResponse> orders = adminOrderService.getOrders(orderNumber, orderStatus, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "주문 상세 조회",
            description = "관리자용 주문 상세 정보를 조회합니다. 주문 상품 목록과 배송 정보를 포함합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = AdminOrderDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "주문을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrderDetail(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId) {
        AdminOrderDetailResponse order = adminOrderService.getOrderDetail(orderId);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "주문 수정",
            description = "관리자가 주문 상태 및 메모를 수정합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정 성공",
                            content = @Content(schema = @Schema(implementation = AdminOrderDetailResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "주문을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PutMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> updateOrder(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId,
            @Valid @RequestBody AdminOrderUpdateRequest request) {
        AdminOrderDetailResponse order = adminOrderService.updateOrder(orderId, request);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "주문 강제 취소 (관리자)",
            description = "관리자가 CREATED 또는 PAID 상태의 주문을 강제 취소합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "취소 성공",
                            content = @Content(schema = @Schema(implementation = CancelOrderResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "주문을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "취소 불가 상태",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        String reason = (request != null) ? request.getCancellationReason() : null;
        CancelOrderResponse response = orderCancellationService.cancelByAdmin(orderId, reason);
        return ResponseEntity.ok(response);
    }
}

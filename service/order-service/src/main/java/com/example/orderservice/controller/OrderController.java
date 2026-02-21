package com.example.orderservice.controller;

import com.example.orderservice.dto.request.CancelOrderRequest;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.response.CancelOrderResponse;
import com.example.orderservice.dto.response.MyOrderResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.global.exception.ErrorResponse;
import com.example.orderservice.service.OrderCancellationService;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;

    @Operation(
            summary = "주문 생성",
            description = "새로운 주문을 생성합니다",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "주문 생성 요청",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OrderCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "주문 생성 예시",
                                    value = """
                                            {
                                              "orderItems": [
                                                {
                                                  "productId": 456,
                                                  "skuId": 789,
                                                  "quantity": 2
                                                }
                                              ],
                                              "deliveryInfo": {
                                                "receiverName": "홍길동",
                                                "receiverPhone": "010-1234-5678",
                                                "zipcode": "12345",
                                                "address": "서울특별시 강남구 테헤란로 123",
                                                "addressDetail": "아파트 101동 202호",
                                                "deliveryMemo": "문 앞에 놓아주세요."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "주문 생성 성공",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "내 주문 목록 조회",
            description = "사용자의 주문 목록을 페이지네이션하여 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(schema = @Schema(implementation = PageResponse.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PageResponse<MyOrderResponse>> getMyOrders(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MyOrderResponse> orders = orderService.getMyOrders(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "주문 취소",
            description = "CREATED 또는 PAID 상태의 주문을 취소합니다. 이미 배송 중인 주문은 취소할 수 없습니다.",
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
                            description = "취소 불가 상태 (배송중, 배송완료 등)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<CancelOrderResponse> cancelOrder(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderRequest request) {
        String reason = (request != null) ? request.getCancellationReason() : null;
        CancelOrderResponse response = orderCancellationService.cancelByUser(userId, orderId, reason);
        return ResponseEntity.ok(response);
    }
}

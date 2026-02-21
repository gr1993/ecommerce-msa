package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;
import com.example.shippingservice.shipping.service.InternalShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 서비스 간 통신용 컨트롤러 (API Gateway 미경유)
 * order-service에서 Feign Client로 호출합니다.
 */
@Tag(name = "Internal Shipping", description = "내부 서비스 간 통신용 배송 API (order-service 전용)")
@RestController
@RequestMapping("/internal/shipping")
@RequiredArgsConstructor
public class InternalShippingController {

    private final InternalShippingService internalShippingService;

    @Operation(
            summary = "배송 취소 가능 여부 확인",
            description = "주문 취소 요청 시 order-service에서 호출하여 배송 취소 가능 여부를 확인합니다. "
                    + "배송사에 전송 완료(SENT)까지는 취소 가능하며, 실제 배송 시작(IN_TRANSIT) 이후로는 취소할 수 없습니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "확인 성공 (cancellable 필드로 가부 판단)")
            }
    )
    @GetMapping("/orders/{orderId}/cancellable")
    public ResponseEntity<ShippingCancellableResponse> checkCancellable(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId) {
        ShippingCancellableResponse response = internalShippingService.checkCancellable(orderId);
        return ResponseEntity.ok(response);
    }
}

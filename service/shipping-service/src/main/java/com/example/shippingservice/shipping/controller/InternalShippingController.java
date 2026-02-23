package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.shipping.dto.request.TestCreateShippingRequest;
import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;
import com.example.shippingservice.shipping.dto.response.TestCreateShippingResponse;
import com.example.shippingservice.shipping.service.InternalShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(
            summary = "[테스트] 배송 정보 생성",
            description = "테스트용 배송 정보 생성 API입니다. order.created 이벤트를 수동으로 시뮬레이션합니다. "
                    + "동일한 orderId로 재요청해도 기존 데이터를 반환하여 멱등성을 보장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "배송 정보 생성 성공")
            }
    )
    @PostMapping("/test/shipping")
    public ResponseEntity<TestCreateShippingResponse> createShippingForTest(
            @Valid @RequestBody TestCreateShippingRequest request) {
        TestCreateShippingResponse response = internalShippingService.createShippingForTest(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[테스트] 배송/반품/교환 정보 삭제",
            description = "테스트 데이터 정리를 위한 삭제 API입니다. "
                    + "주문 ID에 해당하는 배송, 반품, 교환 정보를 모두 삭제합니다. "
                    + "해당 데이터가 없어도 오류를 발생시키지 않습니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공")
            }
    )
    @DeleteMapping("/test/shipping/orders/{orderId}")
    public ResponseEntity<Void> deleteShippingForTest(
            @Parameter(description = "주문 ID", required = true, example = "1")
            @PathVariable Long orderId) {
        internalShippingService.deleteShippingForTest(orderId);
        return ResponseEntity.noContent().build();
    }
}

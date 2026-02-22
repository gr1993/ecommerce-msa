package com.example.shippingservice.exchange.controller;

import com.example.shippingservice.exchange.dto.request.InternalCreateExchangeRequest;
import com.example.shippingservice.exchange.dto.response.InternalCreateExchangeResponse;
import com.example.shippingservice.exchange.service.InternalExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 서비스 간 통신용 교환 컨트롤러 (API Gateway 미경유)
 * order-service에서 Feign Client로 호출합니다.
 */
@Tag(name = "Internal Exchange", description = "내부 서비스 간 통신용 교환 API (order-service 전용)")
@RestController
@RequestMapping("/internal/shipping/exchanges")
@RequiredArgsConstructor
public class InternalExchangeController {

    private final InternalExchangeService internalExchangeService;

    @Operation(
            summary = "교환 레코드 생성",
            description = "order-service에서 교환 신청 시 호출합니다. "
                    + "배송 완료(DELIVERED) 상태이며 진행 중인 반품/교환 건이 없는 경우에만 생성됩니다.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "교환 생성 성공"),
                    @ApiResponse(responseCode = "400", description = "배송 정보 없음 또는 잘못된 요청"),
                    @ApiResponse(responseCode = "409", description = "교환 불가 상태 (배송 미완료, 진행 중인 반품/교환 존재)")
            }
    )
    @PostMapping
    public ResponseEntity<InternalCreateExchangeResponse> createExchange(
            @Valid @RequestBody InternalCreateExchangeRequest request) {
        InternalCreateExchangeResponse response = internalExchangeService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

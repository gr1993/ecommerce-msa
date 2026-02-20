package com.example.shippingservice.shipping.controller;

import com.example.shippingservice.shipping.service.OrderShippingSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order Shipping Sync", description = "배송 대상 주문 동기화 API")
@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
public class OrderShippingSyncController {

    private final OrderShippingSyncService orderShippingSyncService;

    @Operation(summary = "전체 배송 대상 주문 동기화", description = "order-service의 배송 대상 주문(PAID 상태)을 shipping-service에 동기화합니다.")
    @PostMapping("/full")
    public ResponseEntity<FullSyncResponse> fullSync() {
        int syncedCount = orderShippingSyncService.fullSync();
        return ResponseEntity.ok(new FullSyncResponse(syncedCount, "Full sync completed successfully"));
    }

    public record FullSyncResponse(int syncedCount, String message) {}
}

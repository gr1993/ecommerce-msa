package com.example.catalogservice.controller;

import com.example.catalogservice.service.ProductSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Product Sync", description = "상품 동기화 API")
@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
public class ProductSyncController {

    private final ProductSyncService productSyncService;

    @Operation(summary = "전체 상품 동기화", description = "product-service의 모든 상품을 Elasticsearch에 동기화합니다.")
    @PostMapping("/full")
    public ResponseEntity<FullSyncResponse> fullSync() {
        int syncedCount = productSyncService.fullSync();
        return ResponseEntity.ok(new FullSyncResponse(syncedCount, "Full sync completed successfully"));
    }

    public record FullSyncResponse(int syncedCount, String message) {}
}

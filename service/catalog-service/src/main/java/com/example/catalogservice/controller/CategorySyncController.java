package com.example.catalogservice.controller;

import com.example.catalogservice.domain.CategoryCache;
import com.example.catalogservice.service.CategorySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Category Sync", description = "카테고리 동기화 API")
@RestController
@RequestMapping("/api/internal/sync/categories")
@RequiredArgsConstructor
public class CategorySyncController {

    private final CategorySyncService categorySyncService;

    @Operation(summary = "전체 카테고리 동기화", description = "product-service의 모든 카테고리를 Redis에 동기화합니다.")
    @PostMapping("/full")
    public ResponseEntity<FullSyncResponse> fullSync() {
        int syncedCount = categorySyncService.fullSync();
        return ResponseEntity.ok(new FullSyncResponse(syncedCount, "Category full sync completed successfully"));
    }

    @Operation(summary = "전체 카테고리 조회", description = "동기화된 전체 카테고리 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<CategoryCache>> getAllCategories() {
        List<CategoryCache> categories = categorySyncService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "단일 카테고리 조회", description = "카테고리 ID로 단일 카테고리를 조회합니다.")
    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryCache> getCategoryById(@PathVariable("categoryId") Long categoryId) {
        CategoryCache category = categorySyncService.getCategoryById(categoryId);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(category);
    }

    public record FullSyncResponse(int syncedCount, String message) {}
}

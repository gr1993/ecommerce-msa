package com.example.catalogservice.controller;

import com.example.catalogservice.domain.CategoryTreeNode;
import com.example.catalogservice.service.CategorySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category", description = "카테고리 조회 API")
@RestController
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategorySyncService categorySyncService;

    @Operation(summary = "카테고리 트리 조회", description = "계층 구조의 카테고리 트리를 조회합니다.")
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeNode>> getCategoryTree() {
        List<CategoryTreeNode> tree = categorySyncService.getCategoryTree();
        return ResponseEntity.ok(tree);
    }
}

package com.example.catalogservice.client.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogSyncCategoryResponse {

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private Integer displayOrder;
    private Integer depth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

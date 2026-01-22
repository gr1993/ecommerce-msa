package com.example.catalogservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCache implements Serializable {

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private Integer displayOrder;
    private Integer depth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.example.catalogservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryTreeNode implements Serializable {

    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private Integer displayOrder;
    private Integer depth;
    private List<CategoryTreeNode> children;
}

package com.example.catalogservice.controller.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Builder
public class ProductSearchRequest {

    private String productName;
    private Long categoryId;
    private String status;
    private Long minPrice;
    private Long maxPrice;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    private String sort;

    public Pageable toPageable() {
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            return PageRequest.of(page, size, Sort.by(direction, property));
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

package com.example.catalogservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdatedEvent {

    private Long categoryId;
    private String categoryName;
    private Long parentId;
    private Integer displayOrder;
    private Boolean isDisplayed;
    private LocalDateTime updatedAt;
}

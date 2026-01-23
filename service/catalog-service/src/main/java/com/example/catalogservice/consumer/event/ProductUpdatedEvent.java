package com.example.catalogservice.consumer.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {

    private Long productId;
    private String productCode;
    private String productName;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String status;
    private Boolean isDisplayed;
    private List<Long> categoryIds;
    private LocalDateTime updatedAt;
}

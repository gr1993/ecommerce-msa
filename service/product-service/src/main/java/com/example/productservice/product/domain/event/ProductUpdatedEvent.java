package com.example.productservice.product.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {
	private Long productId;
	private String productName;
	private String productCode;
	private String description;
	private BigDecimal basePrice;
	private BigDecimal salePrice;
	private String status;
	private Boolean isDisplayed;
	private List<Long> categoryIds;
	private LocalDateTime updatedAt;
}

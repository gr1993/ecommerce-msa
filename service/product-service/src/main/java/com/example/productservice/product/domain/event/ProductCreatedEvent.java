package com.example.productservice.product.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {
	private Long productId;
	private String productName;
	private String productCode;
	private String description;
	private BigDecimal basePrice;
	private BigDecimal salePrice;
	private String status;
	private Boolean isDisplayed;
	private List<Long> categoryIds;
	private String primaryImageUrl;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	@Schema(description = "생성 일시", example = "2026-01-23T16:58:34.035882", type = "string")
	private LocalDateTime createdAt;
}

package com.example.productservice.category.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreatedEvent {
	private Long categoryId;
	private Long parentId;
	private String categoryName;
	private Integer displayOrder;
	private Boolean isDisplayed;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	@Schema(description = "생성 일시", example = "2026-01-23T16:58:34.035882", type = "string")
	private LocalDateTime createdAt;
}

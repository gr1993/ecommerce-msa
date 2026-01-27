package com.example.productservice.product.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeywordDeletedEvent {

	@Schema(description = "키워드 ID", example = "1")
	private Long keywordId;

	@Schema(description = "상품 ID", example = "100")
	private Long productId;

	@Schema(description = "삭제된 검색 키워드", example = "운동화")
	private String keyword;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	@Schema(description = "삭제 일시", example = "2026-01-27T10:00:00.000000", type = "string")
	private LocalDateTime deletedAt;
}

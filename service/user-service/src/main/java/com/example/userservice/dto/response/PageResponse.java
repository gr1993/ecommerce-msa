package com.example.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "페이지네이션 응답 DTO")
public class PageResponse<T> {
	@Schema(description = "컨텐츠 목록")
	private List<T> content;

	@Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
	private int page;

	@Schema(description = "페이지 크기", example = "10")
	private int size;

	@Schema(description = "전체 요소 개수", example = "100")
	private long totalElements;

	@Schema(description = "전체 페이지 개수", example = "10")
	private int totalPages;

	@Schema(description = "마지막 페이지 여부", example = "false")
	private boolean last;

	public static <T> PageResponse<T> of(Page<T> page) {
		return PageResponse.<T>builder()
				.content(page.getContent())
				.page(page.getNumber())
				.size(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.last(page.isLast())
				.build();
	}
}

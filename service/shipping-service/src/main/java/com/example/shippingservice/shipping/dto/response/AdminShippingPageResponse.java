package com.example.shippingservice.shipping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "관리자 배송 목록 페이지 응답")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminShippingPageResponse {

    @Schema(description = "배송 목록")
    private List<AdminShippingResponse> content;

    @Schema(description = "현재 페이지 번호", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 데이터 개수", example = "100")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPages;

    @Schema(description = "첫 페이지 여부", example = "true")
    private boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private boolean last;
}

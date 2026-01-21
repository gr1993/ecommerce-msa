package com.example.productservice.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카탈로그 동기화 요청")
public class CatalogSyncRequest {

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기", example = "100")
    private Integer size;

    public int getPageOrDefault() {
        return page != null ? page : 0;
    }

    public int getSizeOrDefault() {
        return size != null ? size : 100;
    }
}

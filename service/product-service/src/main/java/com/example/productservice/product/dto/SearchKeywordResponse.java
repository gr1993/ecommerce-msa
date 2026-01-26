package com.example.productservice.product.dto;

import com.example.productservice.product.domain.ProductSearchKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "검색 키워드 응답")
public class SearchKeywordResponse {

    @Schema(description = "키워드 ID", example = "1")
    private Long keywordId;

    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    @Schema(description = "검색 키워드", example = "랩탑")
    private String keyword;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    public static SearchKeywordResponse from(ProductSearchKeyword entity) {
        return SearchKeywordResponse.builder()
                .keywordId(entity.getKeywordId())
                .productId(entity.getProduct().getProductId())
                .keyword(entity.getKeyword())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

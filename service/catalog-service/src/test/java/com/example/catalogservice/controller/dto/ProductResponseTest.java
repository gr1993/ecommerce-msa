package com.example.catalogservice.controller.dto;

import com.example.catalogservice.domain.document.ProductDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseTest {

    @Test
    @DisplayName("ProductDocument를 ProductResponse로 변환 - searchKeywords 포함")
    void from_withSearchKeywords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ProductDocument document = ProductDocument.builder()
                .productId("P001")
                .productName("맥북 프로 16인치")
                .description("애플 M3 맥스 칩 탑재")
                .basePrice(3500000L)
                .salePrice(3300000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/image.jpg")
                .categoryIds(List.of(1L, 10L))
                .searchKeywords(List.of("애플", "노트북", "프로"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        ProductResponse response = ProductResponse.from(document);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("P001");
        assertThat(response.getProductName()).isEqualTo("맥북 프로 16인치");
        assertThat(response.getDescription()).isEqualTo("애플 M3 맥스 칩 탑재");
        assertThat(response.getBasePrice()).isEqualTo(3500000L);
        assertThat(response.getSalePrice()).isEqualTo(3300000L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getPrimaryImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(response.getCategoryIds()).containsExactly(1L, 10L);
        assertThat(response.getSearchKeywords()).containsExactly("애플", "노트북", "프로");
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("ProductDocument를 ProductResponse로 변환 - searchKeywords null")
    void from_withNullSearchKeywords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ProductDocument document = ProductDocument.builder()
                .productId("P002")
                .productName("갤럭시 S24 울트라")
                .description("삼성 최신 플래그십 스마트폰")
                .basePrice(1500000L)
                .salePrice(1400000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/galaxy.jpg")
                .categoryIds(List.of(2L, 20L))
                .searchKeywords(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        ProductResponse response = ProductResponse.from(document);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("P002");
        assertThat(response.getSearchKeywords()).isNull();
    }

    @Test
    @DisplayName("ProductDocument를 ProductResponse로 변환 - searchKeywords 빈 리스트")
    void from_withEmptySearchKeywords() {
        // given
        LocalDateTime now = LocalDateTime.now();
        ProductDocument document = ProductDocument.builder()
                .productId("P003")
                .productName("아이패드 프로 12.9")
                .description("M2 칩 탑재 태블릿")
                .basePrice(1200000L)
                .salePrice(1100000L)
                .status("ACTIVE")
                .primaryImageUrl("https://example.com/ipad.jpg")
                .categoryIds(List.of(1L, 11L))
                .searchKeywords(List.of())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // when
        ProductResponse response = ProductResponse.from(document);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo("P003");
        assertThat(response.getSearchKeywords()).isEmpty();
    }
}

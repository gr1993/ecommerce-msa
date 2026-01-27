package com.example.catalogservice.domain.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
// indexName은 '별칭(Alias)'으로 사용할 이름을 적고,
// createIndex = false를 통해 자동 생성을 막습니다. (ElasticsearchIndexInitializer 클래스에서 생성 로직 구현)
@Document(indexName = "products", createIndex = false)
@Setting(settingPath = "elasticsearch/settings.json")
public class ProductDocument {

    /**
     * ID는 수치 연산보다 조회가 많으므로 keyword로 설정함
     */
    @Id
    @Field(type = FieldType.Keyword)
    private String productId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "korean_analyzer"),
            otherFields = {
                    @InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard")
            }
    )
    private String productName;

    @Field(type = FieldType.Text, analyzer = "korean_analyzer")
    private String description;

    @Field(type = FieldType.Long)
    private Long basePrice;

    @Field(type = FieldType.Long)
    private Long salePrice;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword, index = false) // 검색하지 않는 필드
    private String primaryImageUrl;

    /**
     * Leaf 카테고리 ID 배열
     */
    @Field(type = FieldType.Long)
    private List<Long> categoryIds;

    /**
     * 검색 키워드 배열
     */
    @Field(type = FieldType.Keyword)
    private List<String> searchKeywords;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;
}

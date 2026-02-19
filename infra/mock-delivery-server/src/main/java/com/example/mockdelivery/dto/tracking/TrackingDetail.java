package com.example.mockdelivery.dto.tracking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "배송 상세 이력")
public class TrackingDetail {

    @Schema(description = "처리 시간", example = "2026-02-19 10:00")
    private String timeString;

    @Schema(description = "처리 장소", example = "서울 강남")
    private String where;

    @Schema(description = "처리 내용", example = "배송 준비중")
    private String remark;

    @Schema(description = "처리 상태", example = "배송중")
    private String kind;
}

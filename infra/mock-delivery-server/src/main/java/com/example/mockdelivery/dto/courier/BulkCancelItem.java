package com.example.mockdelivery.dto.courier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "송장 취소 항목")
public class BulkCancelItem {

    @Schema(description = "취소할 송장 번호", example = "100000000000")
    private String trackingNumber;
}

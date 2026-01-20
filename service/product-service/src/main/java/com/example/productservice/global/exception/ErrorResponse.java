package com.example.productservice.global.exception;

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
@Schema(description = "에러 응답")
public class ErrorResponse {

    @Schema(description = "발생 시각", example = "2024-01-01T10:00:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP 상태 코드", example = "400")
    private int status;

    @Schema(description = "에러 유형", example = "Bad Request")
    private String error;

    @Schema(description = "에러 메시지", example = "카테고리 '의류'(ID: 1)는 3단계 카테고리가 아닙니다.")
    private String message;
}

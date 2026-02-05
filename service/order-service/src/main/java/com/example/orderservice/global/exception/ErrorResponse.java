package com.example.orderservice.global.exception;

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

    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
    private String message;
}

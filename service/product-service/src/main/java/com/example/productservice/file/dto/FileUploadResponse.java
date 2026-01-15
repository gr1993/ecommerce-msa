package com.example.productservice.file.dto;

import com.example.productservice.file.domain.FileUpload;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "파일 업로드 응답")
public class FileUploadResponse {

    @Schema(description = "파일 ID", example = "1")
    private Long fileId;

    @Schema(description = "원본 파일명", example = "product.jpg")
    private String originalFilename;

    @Schema(description = "파일 URL", example = "/files/temp/abc123.jpg")
    private String url;

    @Schema(description = "파일 크기 (bytes)", example = "102400")
    private Long fileSize;

    @Schema(description = "파일 타입", example = "image/jpeg")
    private String contentType;

    @Schema(description = "상태", example = "TEMP")
    private String status;

    @Schema(description = "업로드 일시", example = "2024-01-01T10:00:00")
    private LocalDateTime uploadedAt;

    public static FileUploadResponse from(FileUpload fileUpload) {
        return FileUploadResponse.builder()
                .fileId(fileUpload.getFileId())
                .originalFilename(fileUpload.getOriginalFilename())
                .url(fileUpload.getUrl())
                .fileSize(fileUpload.getFileSize())
                .contentType(fileUpload.getContentType())
                .status(fileUpload.getStatus())
                .uploadedAt(fileUpload.getUploadedAt())
                .build();
    }
}

package com.example.productservice.service;

import com.example.productservice.domain.entity.FileUpload;
import com.example.productservice.domain.repository.FileUploadRepository;
import com.example.productservice.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FileStorageServiceImpl implements FileStorageService {

    private final FileUploadRepository fileUploadRepository;

    @Value("${file.upload.temp-dir:uploads/temp}")
    private String tempDir;

    @Value("${file.upload.confirmed-dir:uploads/images}")
    private String confirmedDir;

    @Value("${file.upload.base-url:/files}")
    private String baseUrl;

    @Override
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 없습니다");
        }

        // 파일 확장자 검증
        String extension = getExtension(originalFilename);
        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식입니다: " + extension);
        }

        try {
            // 날짜별 디렉토리 생성 (예: 2024/01/15)
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path tempPath = Paths.get(tempDir, dateDir);
            Files.createDirectories(tempPath);

            // 고유 파일명 생성
            String storedFilename = UUID.randomUUID() + "." + extension;
            Path filePath = tempPath.resolve(storedFilename);

            // 파일 저장
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL 생성
            String url = baseUrl + "/temp/" + dateDir + "/" + storedFilename;

            // DB에 기록
            FileUpload fileUpload = FileUpload.builder()
                    .originalFilename(originalFilename)
                    .storedFilename(storedFilename)
                    .filePath(filePath.toString())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .status("TEMP")
                    .url(url)
                    .build();

            FileUpload savedFile = fileUploadRepository.save(fileUpload);

            log.info("File uploaded successfully: {} -> {}", originalFilename, storedFilename);

            return FileUploadResponse.from(savedFile);

        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new RuntimeException("파일 업로드에 실패했습니다", e);
        }
    }

    @Override
    @Transactional
    public Map<Long, String> confirmFiles(List<Long> fileIds) {
        Map<Long, String> idUrlMap = new HashMap<>();
        if (fileIds == null || fileIds.isEmpty()) {
            return idUrlMap;
        }

        List<FileUpload> files = fileUploadRepository.findAllById(fileIds);

        for (FileUpload file : files) {
            if (!"TEMP".equals(file.getStatus())) {
                log.warn("File is not in TEMP status: {}", file.getFileId());
                continue;
            }

            try {
                // 임시 폴더에서 실제 폴더로 이동
                Path tempPath = Paths.get(file.getFilePath());

                // 날짜별 디렉토리 유지
                String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                Path confirmedPath = Paths.get(confirmedDir, dateDir);
                Files.createDirectories(confirmedPath);

                Path newFilePath = confirmedPath.resolve(file.getStoredFilename());
                Files.move(tempPath, newFilePath, StandardCopyOption.REPLACE_EXISTING);

                // URL 업데이트
                String newUrl = baseUrl + "/images/" + dateDir + "/" + file.getStoredFilename();

                // DB 상태 변경
                file.setStatus("CONFIRMED");
                file.setConfirmedAt(LocalDateTime.now());
                file.setFilePath(newFilePath.toString());
                file.setUrl(newUrl);

                log.info("File confirmed: {} -> {}", file.getFileId(), newUrl);
                idUrlMap.put(file.getFileId(), newUrl);

            } catch (IOException e) {
                log.error("Failed to move file: {}", file.getFileId(), e);
                // 이동 실패해도 계속 진행 (일부 파일만 실패할 수 있음)
            }
        }
        return idUrlMap;
    }

    @Override
    @Transactional
    public void deleteOrphanFiles() {
        // 24시간 이상 경과한 TEMP 파일 조회
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<FileUpload> orphanFiles = fileUploadRepository.findByStatusAndUploadedAtBefore("TEMP", threshold);

        log.info("Found {} orphan files to delete", orphanFiles.size());

        for (FileUpload file : orphanFiles) {
            try {
                // 실제 파일 삭제
                Path filePath = Paths.get(file.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted orphan file: {}", filePath);
                }

                // DB 상태 변경
                file.setStatus("DELETED");

            } catch (IOException e) {
                log.error("Failed to delete orphan file: {}", file.getFileId(), e);
            }
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    private boolean isAllowedExtension(String extension) {
        List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "gif", "webp");
        return allowedExtensions.contains(extension.toLowerCase());
    }
}

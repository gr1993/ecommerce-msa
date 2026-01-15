package com.example.productservice.file.service;

import com.example.productservice.file.domain.FileUpload;
import com.example.productservice.file.dto.FileUploadResponse;
import com.example.productservice.file.repository.FileUploadRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageServiceImpl 테스트")
class FileStorageServiceImplTest {

    @Mock
    private FileUploadRepository fileUploadRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDirectory;

    private String tempDir;
    private String confirmedDir;

    @BeforeEach
    void setUp() {
        tempDir = tempDirectory.resolve("temp").toString();
        confirmedDir = tempDirectory.resolve("confirmed").toString();

        ReflectionTestUtils.setField(fileStorageService, "tempDir", tempDir);
        ReflectionTestUtils.setField(fileStorageService, "confirmedDir", confirmedDir);
        ReflectionTestUtils.setField(fileStorageService, "baseUrl", "/files");
    }

    @AfterEach
    void tearDown() throws IOException {
        // 테스트 후 임시 디렉토리 정리
        if (Files.exists(tempDirectory)) {
            Files.walk(tempDirectory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    @Test
    @DisplayName("파일 업로드 - 성공")
    void uploadFile_success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        FileUpload savedFile = FileUpload.builder()
                .fileId(1L)
                .originalFilename("test-image.jpg")
                .storedFilename("uuid-test.jpg")
                .filePath(tempDir + "/2024/01/15/uuid-test.jpg")
                .fileSize(18L)
                .contentType("image/jpeg")
                .status("TEMP")
                .url("/files/temp/2024/01/15/uuid-test.jpg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileUploadRepository.save(any(FileUpload.class))).thenReturn(savedFile);

        // when
        FileUploadResponse response = fileStorageService.uploadFile(file);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(1L);
        assertThat(response.getOriginalFilename()).isEqualTo("test-image.jpg");
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getStatus()).isEqualTo("TEMP");
        assertThat(response.getUrl()).contains("/files/temp/");

        ArgumentCaptor<FileUpload> captor = ArgumentCaptor.forClass(FileUpload.class);
        verify(fileUploadRepository, times(1)).save(captor.capture());

        FileUpload capturedFile = captor.getValue();
        assertThat(capturedFile.getOriginalFilename()).isEqualTo("test-image.jpg");
        assertThat(capturedFile.getStatus()).isEqualTo("TEMP");
    }

    @Test
    @DisplayName("파일 업로드 - 빈 파일")
    void uploadFile_emptyFile() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일이 비어있습니다");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("파일 업로드 - 파일명 없음")
    void uploadFile_noFilename() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("파일명이 없습니다");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("파일 업로드 - 허용되지 않은 확장자")
    void uploadFile_invalidExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> fileStorageService.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 파일 형식입니다");

        verify(fileUploadRepository, never()).save(any());
    }

    @Test
    @DisplayName("파일 업로드 - 허용된 이미지 확장자들")
    void uploadFile_allowedExtensions() {
        // given
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

        for (String ext : allowedExtensions) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test." + ext,
                    "image/" + ext,
                    "test content".getBytes()
            );

            FileUpload savedFile = FileUpload.builder()
                    .fileId(1L)
                    .originalFilename("test." + ext)
                    .storedFilename("uuid-test." + ext)
                    .filePath(tempDir + "/uuid-test." + ext)
                    .fileSize(12L)
                    .contentType("image/" + ext)
                    .status("TEMP")
                    .url("/files/temp/uuid-test." + ext)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(fileUploadRepository.save(any(FileUpload.class))).thenReturn(savedFile);

            // when
            FileUploadResponse response = fileStorageService.uploadFile(file);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalFilename()).isEqualTo("test." + ext);
        }

        verify(fileUploadRepository, times(allowedExtensions.size())).save(any());
    }

    @Test
    @DisplayName("파일 확정 - 성공")
    void confirmFiles_success() throws IOException {
        // given
        // 임시 파일 생성
        String dateDir = "2024/01/15";
        Path tempPath = Paths.get(tempDir, dateDir);
        Files.createDirectories(tempPath);
        Path tempFile = tempPath.resolve("test-uuid.jpg");
        Files.write(tempFile, "test content".getBytes());

        FileUpload file = FileUpload.builder()
                .fileId(1L)
                .originalFilename("test.jpg")
                .storedFilename("test-uuid.jpg")
                .filePath(tempFile.toString())
                .fileSize(12L)
                .contentType("image/jpeg")
                .status("TEMP")
                .url("/files/temp/2024/01/15/test-uuid.jpg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileUploadRepository.findAllById(Arrays.asList(1L))).thenReturn(Arrays.asList(file));

        // when
        fileStorageService.confirmFiles(Arrays.asList(1L));

        // then
        assertThat(file.getStatus()).isEqualTo("CONFIRMED");
        assertThat(file.getConfirmedAt()).isNotNull();
        assertThat(file.getUrl()).contains("/files/images/");
        assertThat(Files.exists(tempFile)).isFalse(); // 원본 파일은 삭제됨
    }

    @Test
    @DisplayName("파일 확정 - 빈 목록")
    void confirmFiles_emptyList() {
        // when
        fileStorageService.confirmFiles(null);
        fileStorageService.confirmFiles(Arrays.asList());

        // then
        verify(fileUploadRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("파일 확정 - TEMP 상태가 아닌 파일은 건너뜀")
    void confirmFiles_skipNonTempFiles() {
        // given
        FileUpload file = FileUpload.builder()
                .fileId(1L)
                .originalFilename("test.jpg")
                .storedFilename("test-uuid.jpg")
                .filePath(tempDir + "/test-uuid.jpg")
                .fileSize(12L)
                .contentType("image/jpeg")
                .status("CONFIRMED")
                .url("/files/images/test-uuid.jpg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileUploadRepository.findAllById(Arrays.asList(1L))).thenReturn(Arrays.asList(file));

        // when
        fileStorageService.confirmFiles(Arrays.asList(1L));

        // then
        assertThat(file.getStatus()).isEqualTo("CONFIRMED"); // 상태 변경 없음
        assertThat(file.getConfirmedAt()).isNull(); // 확정 시간 설정 안됨
    }

    @Test
    @DisplayName("고아 파일 삭제 - 성공")
    void deleteOrphanFiles_success() throws IOException {
        // given
        // 삭제할 파일 생성
        Path orphanFile = Paths.get(tempDir, "orphan-file.jpg");
        Files.createDirectories(orphanFile.getParent());
        Files.write(orphanFile, "orphan content".getBytes());

        FileUpload file = FileUpload.builder()
                .fileId(1L)
                .originalFilename("orphan.jpg")
                .storedFilename("orphan-file.jpg")
                .filePath(orphanFile.toString())
                .fileSize(14L)
                .contentType("image/jpeg")
                .status("TEMP")
                .url("/files/temp/orphan-file.jpg")
                .uploadedAt(LocalDateTime.now().minusHours(25))
                .build();

        when(fileUploadRepository.findByStatusAndUploadedAtBefore(eq("TEMP"), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(file));

        // when
        fileStorageService.deleteOrphanFiles();

        // then
        assertThat(file.getStatus()).isEqualTo("DELETED");
        assertThat(Files.exists(orphanFile)).isFalse();
    }

    @Test
    @DisplayName("고아 파일 삭제 - 파일이 없으면 건너뜀")
    void deleteOrphanFiles_fileNotExists() {
        // given
        FileUpload file = FileUpload.builder()
                .fileId(1L)
                .originalFilename("nonexistent.jpg")
                .storedFilename("nonexistent.jpg")
                .filePath(tempDir + "/nonexistent.jpg")
                .fileSize(14L)
                .contentType("image/jpeg")
                .status("TEMP")
                .url("/files/temp/nonexistent.jpg")
                .uploadedAt(LocalDateTime.now().minusHours(25))
                .build();

        when(fileUploadRepository.findByStatusAndUploadedAtBefore(eq("TEMP"), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(file));

        // when
        fileStorageService.deleteOrphanFiles();

        // then
        assertThat(file.getStatus()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("고아 파일 삭제 - 고아 파일 없음")
    void deleteOrphanFiles_noOrphanFiles() {
        // given
        when(fileUploadRepository.findByStatusAndUploadedAtBefore(eq("TEMP"), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList());

        // when
        fileStorageService.deleteOrphanFiles();

        // then
        verify(fileUploadRepository, times(1)).findByStatusAndUploadedAtBefore(eq("TEMP"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("파일 업로드 - 대소문자 혼합 확장자")
    void uploadFile_mixedCaseExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.JPG",
                "image/jpeg",
                "test content".getBytes()
        );

        FileUpload savedFile = FileUpload.builder()
                .fileId(1L)
                .originalFilename("test.JPG")
                .storedFilename("uuid-test.jpg")
                .filePath(tempDir + "/uuid-test.jpg")
                .fileSize(12L)
                .contentType("image/jpeg")
                .status("TEMP")
                .url("/files/temp/uuid-test.jpg")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(fileUploadRepository.save(any(FileUpload.class))).thenReturn(savedFile);

        // when
        FileUploadResponse response = fileStorageService.uploadFile(file);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOriginalFilename()).isEqualTo("test.JPG");
        verify(fileUploadRepository, times(1)).save(any());
    }
}

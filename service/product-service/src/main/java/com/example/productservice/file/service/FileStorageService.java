package com.example.productservice.file.service;

import com.example.productservice.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileStorageService {

    FileUploadResponse uploadFile(MultipartFile file);

    Map<Long, String> confirmFiles(List<Long> fileIds);

    void deleteOrphanFiles();
}

package com.example.productservice.service;

import com.example.productservice.domain.entity.FileUpload;
import com.example.productservice.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    FileUploadResponse uploadFile(MultipartFile file);

    void confirmFiles(List<Long> fileIds);

    void deleteOrphanFiles();
}

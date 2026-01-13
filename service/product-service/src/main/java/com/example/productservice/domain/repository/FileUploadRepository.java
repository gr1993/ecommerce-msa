package com.example.productservice.domain.repository;

import com.example.productservice.domain.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByStatusAndUploadedAtBefore(String status, LocalDateTime uploadedAt);
}

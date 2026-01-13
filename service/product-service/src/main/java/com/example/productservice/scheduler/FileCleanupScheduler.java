package com.example.productservice.scheduler;

import com.example.productservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCleanupScheduler {

    private final FileStorageService fileStorageService;

    /**
     * 매일 새벽 3시에 orphan 파일 정리
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanFiles() {
        log.info("Starting orphan file cleanup task");
        try {
            fileStorageService.deleteOrphanFiles();
            log.info("Orphan file cleanup completed successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup orphan files", e);
        }
    }
}

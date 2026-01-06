package com.example.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DLQ(Dead Letter Queue)에 전송된 실패 메시지 엔티티
 * 재처리 및 모니터링을 위한 실패 메시지 저장
 */
@Entity
@Table(name = "failed_messages",
    indexes = {
        @Index(name = "idx_topic", columnList = "topic"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_failed_at", columnList = "failed_at"),
        @Index(name = "idx_event_type", columnList = "event_type")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic", nullable = false, length = 255)
    private String topic;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "partition_number")
    private Integer partition;

    @Column(name = "offset_number")
    private Long offset;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FailedMessageStatus status = FailedMessageStatus.PENDING;

    @CreationTimestamp
    @Column(name = "failed_at", nullable = false, updatable = false)
    private LocalDateTime failedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Builder
    public FailedMessage(String topic, String eventType, Integer partition, Long offset,
                         String payload, String exceptionMessage, String stackTrace) {
        this.topic = topic;
        this.eventType = eventType;
        this.partition = partition;
        this.offset = offset;
        this.payload = payload;
        this.exceptionMessage = exceptionMessage;
        this.stackTrace = stackTrace;
        this.status = FailedMessageStatus.PENDING;
        this.retryCount = 0;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }

    public void markAsProcessed(String memo) {
        this.status = FailedMessageStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.memo = memo;
    }

    public void markAsIgnored(String memo) {
        this.status = FailedMessageStatus.IGNORED;
        this.processedAt = LocalDateTime.now();
        this.memo = memo;
    }

    public void markAsRetryFailed(String memo) {
        this.status = FailedMessageStatus.RETRY_FAILED;
        this.lastRetryAt = LocalDateTime.now();
        this.memo = memo;
    }

    public enum FailedMessageStatus {
        PENDING,
        PROCESSING,
        PROCESSED,
        RETRY_FAILED,
        IGNORED
    }
}

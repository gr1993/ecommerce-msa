package com.example.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 처리된 이벤트 추적 엔티티
 * 이벤트 중복 처리 방지 및 이력 관리를 위한 엔티티
 */
@Entity
@Table(name = "processed_events",
    indexes = {
        @Index(name = "idx_event_type_event_key", columnList = "event_type,event_key", unique = true),
        @Index(name = "idx_processed_at", columnList = "processed_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이벤트 타입 (예: USER_REGISTERED, ORDER_CREATED 등)
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * 이벤트 키 (예: userId, orderId 등 - 이벤트별 고유 식별자)
     */
    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    /**
     * 원본 이벤트 페이로드 (디버깅 및 추적용)
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /**
     * 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProcessStatus status;

    /**
     * 처리 시간
     */
    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    /**
     * 처리 결과 메시지 (성공/실패 메시지)
     */
    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Builder
    public ProcessedEvent(String eventType, String eventKey, String payload,
                          ProcessStatus status, String resultMessage) {
        this.eventType = eventType;
        this.eventKey = eventKey;
        this.payload = payload;
        this.status = status != null ? status : ProcessStatus.SUCCESS;
        this.resultMessage = resultMessage;
    }

    public void updateStatus(ProcessStatus status, String resultMessage) {
        this.status = status;
        this.resultMessage = resultMessage;
    }

    public enum ProcessStatus {
        SUCCESS,    // 정상 처리됨
        DUPLICATE,  // 중복 이벤트 (이미 처리됨)
        FAILED      // 처리 실패 (DLQ로 이동)
    }
}

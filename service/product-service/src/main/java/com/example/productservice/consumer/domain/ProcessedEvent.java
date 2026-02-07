package com.example.productservice.consumer.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 처리된 이벤트 추적 엔티티
 * Kafka 메시지 재처리 방지를 위한 멱등성 보장
 *
 * 지원 이벤트:
 * - ORDER_CREATED: 주문 생성 (재고 차감)
 * - ORDER_CANCELLED: 주문 취소 (재고 복구 보상 트랜잭션)
 * - PAYMENT_CANCELLED: 결제 취소 (재고 복구 보상 트랜잭션)
 * - 향후 확장 가능
 */
@Entity
@Table(
        name = "processed_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_type_aggregate_id",
                columnNames = {"event_type", "aggregate_id"}
        ),
        indexes = {
                @Index(name = "idx_processed_at", columnList = "processed_at"),
                @Index(name = "idx_event_type", columnList = "event_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("처리 이력 ID")
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    @Comment("이벤트 타입 (ORDER_CREATED, ORDER_CANCELLED, PAYMENT_CANCELLED 등)")
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    @Comment("이벤트 대상 ID (주문 ID, SKU ID 등)")
    private String aggregateId;

    @Column(name = "event_payload", columnDefinition = "TEXT")
    @Comment("이벤트 페이로드 (선택사항, 디버깅용)")
    private String eventPayload;

    @Column(name = "processed_at", nullable = false)
    @Comment("처리 완료 시각")
    private LocalDateTime processedAt;

    /**
     * 주문 생성 이벤트 처리 이력 생성
     */
    public static ProcessedEvent ofOrderCreated(Long orderId, String orderNumber) {
        return ProcessedEvent.builder()
                .eventType("ORDER_CREATED")
                .aggregateId(orderId.toString())
                .eventPayload(String.format("{\"orderId\":%d,\"orderNumber\":\"%s\"}", orderId, orderNumber))
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 취소 이벤트 처리 이력 생성 (보상 트랜잭션)
     */
    public static ProcessedEvent ofOrderCancelled(Long orderId, String reason) {
        return ProcessedEvent.builder()
                .eventType("ORDER_CANCELLED")
                .aggregateId(orderId.toString())
                .eventPayload(String.format("{\"orderId\":%d,\"reason\":\"%s\"}", orderId, reason))
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 결제 취소 이벤트 처리 이력 생성 (보상 트랜잭션)
     */
    public static ProcessedEvent ofPaymentCancelled(Long orderId, Long paymentId, String reason) {
        return ProcessedEvent.builder()
                .eventType("PAYMENT_CANCELLED")
                .aggregateId(orderId.toString())
                .eventPayload(String.format("{\"orderId\":%d,\"paymentId\":%d,\"reason\":\"%s\"}", orderId, paymentId, reason))
                .processedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 범용 이벤트 처리 이력 생성
     */
    public static ProcessedEvent of(String eventType, String aggregateId, String payload) {
        return ProcessedEvent.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .eventPayload(payload)
                .processedAt(LocalDateTime.now())
                .build();
    }
}

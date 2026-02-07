package com.example.productservice.consumer.repository;

import com.example.productservice.consumer.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * 이벤트 타입과 대상 ID로 처리 이력 조회
     */
    Optional<ProcessedEvent> findByEventTypeAndAggregateId(String eventType, String aggregateId);

    /**
     * 이벤트 타입과 대상 ID로 처리 여부 확인
     */
    boolean existsByEventTypeAndAggregateId(String eventType, String aggregateId);
}

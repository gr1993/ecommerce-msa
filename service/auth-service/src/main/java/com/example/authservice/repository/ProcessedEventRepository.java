package com.example.authservice.repository;

import com.example.authservice.domain.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * 이벤트 타입과 키로 처리 이력 조회
     */
    Optional<ProcessedEvent> findByEventTypeAndEventKey(String eventType, String eventKey);

    /**
     * 이벤트 처리 여부 확인
     */
    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);

    /**
     * 특정 시간 이전의 처리 이력 삭제 (이력 정리용)
     */
    void deleteByProcessedAtBefore(LocalDateTime dateTime);
}

package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByStatus(OutboxStatus status);

    List<Outbox> findByEventTypeAndStatus(String eventType, OutboxStatus status);

    List<Outbox> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    List<Outbox> findByStatusAndCreatedAtBefore(OutboxStatus status, LocalDateTime dateTime);

    List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}

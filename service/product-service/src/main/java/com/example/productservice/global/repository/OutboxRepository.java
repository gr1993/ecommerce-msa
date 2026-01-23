package com.example.productservice.global.repository;

import com.example.productservice.global.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

	List<Outbox> findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus status);
}

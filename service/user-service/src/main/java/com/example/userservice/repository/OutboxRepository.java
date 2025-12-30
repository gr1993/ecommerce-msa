package com.example.userservice.repository;

import com.example.userservice.domain.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
	
	List<Outbox> findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus status);
}


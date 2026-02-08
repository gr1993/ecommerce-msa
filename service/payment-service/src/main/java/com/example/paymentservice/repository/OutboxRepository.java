package com.example.paymentservice.repository;

import com.example.paymentservice.domain.entity.Outbox;
import com.example.paymentservice.domain.entity.OutboxStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxRepository extends MongoRepository<Outbox, String> {

	List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}

package com.example.paymentservice.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Document(collection = "outboxes")
@CompoundIndex(name = "idx_event_type_status", def = "{'eventType': 1, 'status': 1}")
public class Outbox {

	@Id
	private String id;

	private String aggregateType;
	private String aggregateId;
	private String eventType;
	private String payload;
	private OutboxStatus status;
	private LocalDateTime createdAt;
	private LocalDateTime publishedAt;

	@Builder
	public Outbox(String aggregateType, String aggregateId, String eventType, String payload) {
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.status = OutboxStatus.PENDING;
		this.createdAt = LocalDateTime.now();
	}

	public void markAsPublished() {
		this.status = OutboxStatus.PUBLISHED;
		this.publishedAt = LocalDateTime.now();
	}

	public void markAsFailed() {
		this.status = OutboxStatus.FAILED;
	}
}

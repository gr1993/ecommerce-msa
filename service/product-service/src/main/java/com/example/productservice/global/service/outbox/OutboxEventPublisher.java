package com.example.productservice.global.service.outbox;

import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

	private final OutboxRepository outboxRepository;
	private final ProductEventPublisher productEventPublisher;
	private final CategoryEventPublisher categoryEventPublisher;
	private final KeywordEventPublisher keywordEventPublisher;
	private final StockEventPublisher stockEventPublisher;

	/**
	 * DB 트랜잭션으로 Outbox 상태를 관리하고 Kafka 트랜잭션은
	 * DB-Kafka 간 분산 트랜잭션을 만들지 않기 위해 사용하지 않는다.
	 */
	@Transactional
	public void publishPendingEvents() {
		List<Outbox> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(Outbox.OutboxStatus.PENDING);

		if (pendingEvents.isEmpty()) {
			log.debug("발행할 PENDING 이벤트가 없습니다.");
			return;
		}

		log.info("발행할 PENDING 이벤트 {}개를 찾았습니다.", pendingEvents.size());

		for (Outbox outbox : pendingEvents) {
			try {
				publishEventByType(outbox);
				outbox.markAsPublished();
				outboxRepository.save(outbox);
				log.info("이벤트 발행 성공: id={}, eventType={}, aggregateId={}",
					outbox.getId(), outbox.getEventType(), outbox.getAggregateId());
			} catch (Exception e) {
				log.error("이벤트 발행 실패: id={}, eventType={}, aggregateId={}",
					outbox.getId(), outbox.getEventType(), outbox.getAggregateId(), e);
				outbox.markAsFailed();
				outboxRepository.save(outbox);
			}
		}
	}

	private void publishEventByType(Outbox outbox) throws Exception {
		String eventType = outbox.getEventType();

		switch (eventType) {
			// Product Events
			case EventTypeConstants.TOPIC_PRODUCT_CREATED ->
				productEventPublisher.publishProductCreatedEvent(outbox);
			case EventTypeConstants.TOPIC_PRODUCT_UPDATED ->
				productEventPublisher.publishProductUpdatedEvent(outbox);

			// Category Events
			case EventTypeConstants.TOPIC_CATEGORY_CREATED ->
				categoryEventPublisher.publishCategoryCreatedEvent(outbox);
			case EventTypeConstants.TOPIC_CATEGORY_UPDATED ->
				categoryEventPublisher.publishCategoryUpdatedEvent(outbox);
			case EventTypeConstants.TOPIC_CATEGORY_DELETED ->
				categoryEventPublisher.publishCategoryDeletedEvent(outbox);

			// Keyword Events
			case EventTypeConstants.TOPIC_KEYWORD_CREATED ->
				keywordEventPublisher.publishKeywordCreatedEvent(outbox);
			case EventTypeConstants.TOPIC_KEYWORD_DELETED ->
				keywordEventPublisher.publishKeywordDeletedEvent(outbox);

			// Stock Events (Compensation)
			case EventTypeConstants.TOPIC_STOCK_REJECTED ->
				stockEventPublisher.publishStockRejectedEvent(outbox);

			default -> {
				log.warn("알 수 없는 이벤트 타입: {}", eventType);
				throw new IllegalArgumentException("알 수 없는 이벤트 타입: " + eventType);
			}
		}
	}
}

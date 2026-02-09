package com.example.paymentservice.global.service.outbox;

import com.example.paymentservice.domain.entity.Outbox;
import com.example.paymentservice.domain.entity.OutboxStatus;
import com.example.paymentservice.global.common.EventTypeConstants;
import com.example.paymentservice.repository.OutboxRepository;
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
	private final PaymentEventPublisher paymentEventPublisher;

	@Transactional
	public void publishPendingEvents() {
		List<Outbox> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

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

	private void publishEventByType(Outbox outbox) {
		String eventType = outbox.getEventType();

		switch (eventType) {
			case EventTypeConstants.TOPIC_PAYMENT_CANCELLED ->
				paymentEventPublisher.publishPaymentCancelledEvent(outbox);

			case EventTypeConstants.TOPIC_PAYMENT_CONFIRMED ->
				paymentEventPublisher.publishPaymentConfirmedEvent(outbox);

			default -> {
				log.warn("알 수 없는 이벤트 타입: {}", eventType);
				throw new IllegalArgumentException("알 수 없는 이벤트 타입: " + eventType);
			}
		}
	}
}

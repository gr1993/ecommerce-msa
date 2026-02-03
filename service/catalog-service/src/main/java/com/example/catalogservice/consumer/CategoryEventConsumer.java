package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.CategoryCreatedEvent;
import com.example.catalogservice.consumer.event.CategoryDeletedEvent;
import com.example.catalogservice.consumer.event.CategoryUpdatedEvent;
import com.example.catalogservice.service.CategorySyncService;
import io.github.springwolf.bindings.kafka.annotations.KafkaAsyncOperationBinding;
import io.github.springwolf.core.asyncapi.annotations.AsyncListener;
import io.github.springwolf.core.asyncapi.annotations.AsyncMessage;
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Category 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - Redis는 key 기반 upsert로 동작하므로
 *   같은 categoryId로 여러 번 저장해도 결과가 동일함 (자연적 멱등성)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryEventConsumer {

    private final CategorySyncService categorySyncService;

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "category.created",
                    description = "카테고리 등록 이벤트 구독 - Redis에 신규 카테고리 저장",
                    message = @AsyncMessage(
                            messageId = "categoryCreatedEvent",
                            name = "CategoryCreatedEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "category.created", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeCategoryCreatedEvent(
            @Payload CategoryCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received category.created event: categoryId={}, topic={}, offset={}",
                event.getCategoryId(), topic, offset);

        try {
            categorySyncService.syncCategory(event);
            log.info("Successfully processed category.created event: categoryId={}", event.getCategoryId());
        } catch (Exception e) {
            log.error("Failed to process category.created event: categoryId={}", event.getCategoryId(), e);
            throw e;
        }
    }

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "category.updated",
                    description = "카테고리 수정 이벤트 구독 - Redis에 카테고리 정보 갱신",
                    message = @AsyncMessage(
                            messageId = "categoryUpdatedEvent",
                            name = "CategoryUpdatedEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "category.updated", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeCategoryUpdatedEvent(
            @Payload CategoryUpdatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received category.updated event: categoryId={}, topic={}, offset={}",
                event.getCategoryId(), topic, offset);

        try {
            categorySyncService.updateCategory(event);
            log.info("Successfully processed category.updated event: categoryId={}", event.getCategoryId());
        } catch (Exception e) {
            log.error("Failed to process category.updated event: categoryId={}", event.getCategoryId(), e);
            throw e;
        }
    }

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "category.deleted",
                    description = "카테고리 삭제 이벤트 구독 - Redis에서 카테고리 제거",
                    message = @AsyncMessage(
                            messageId = "categoryDeletedEvent",
                            name = "CategoryDeletedEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "category.deleted", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeCategoryDeletedEvent(
            @Payload CategoryDeletedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received category.deleted event: categoryId={}, topic={}, offset={}",
                event.getCategoryId(), topic, offset);

        try {
            categorySyncService.deleteCategory(event);
            log.info("Successfully processed category.deleted event: categoryId={}", event.getCategoryId());
        } catch (Exception e) {
            log.error("Failed to process category.deleted event: categoryId={}", event.getCategoryId(), e);
            throw e;
        }
    }

    /**
     * DLQ(Dead Letter Queue) 핸들러
     * 모든 재시도가 실패한 후 호출됩니다.
     */
    @DltHandler
    public void handleDlt(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
            @Header(value = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
    ) {
        log.error("""
                ========================================
                DLQ 메시지 수신 (재시도 실패)
                ========================================
                DLT Topic: {}
                Original Topic: {}
                Offset: {}
                Payload: {}
                Exception: {}
                ========================================
                """, topic, originalTopic, offset, payload, exceptionMessage);

        if (payload instanceof CategoryCreatedEvent event) {
            log.error("DLQ 처리 필요 - category.created 실패: categoryId={}, categoryName={}",
                    event.getCategoryId(), event.getCategoryName());
        } else if (payload instanceof CategoryUpdatedEvent event) {
            log.error("DLQ 처리 필요 - category.updated 실패: categoryId={}, categoryName={}",
                    event.getCategoryId(), event.getCategoryName());
        } else if (payload instanceof CategoryDeletedEvent event) {
            log.error("DLQ 처리 필요 - category.deleted 실패: categoryId={}",
                    event.getCategoryId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

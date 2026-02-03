package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.KeywordCreatedEvent;
import com.example.catalogservice.consumer.event.KeywordDeletedEvent;
import com.example.catalogservice.service.KeywordSyncService;
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
 * Keyword 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - 키워드 추가: 중복 키워드는 추가하지 않으므로 여러 번 실행해도 동일 결과
 * - 키워드 삭제: 이미 없는 키워드 삭제 시도는 무해함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordEventConsumer {

    private final KeywordSyncService keywordSyncService;

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "keyword.created",
                    description = "검색 키워드 등록 이벤트 구독 - Elasticsearch 상품 문서에 키워드 추가",
                    message = @AsyncMessage(
                            messageId = "keywordCreatedEvent",
                            name = "KeywordCreatedEvent"
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
    @KafkaListener(topics = "keyword.created", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeKeywordCreatedEvent(
            @Payload KeywordCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received keyword.created event: keywordId={}, productId={}, keyword={}, topic={}, offset={}",
                event.getKeywordId(), event.getProductId(), event.getKeyword(), topic, offset);

        try {
            keywordSyncService.addKeyword(event);
            log.info("Successfully processed keyword.created event: keywordId={}", event.getKeywordId());
        } catch (Exception e) {
            log.error("Failed to process keyword.created event: keywordId={}, productId={}",
                    event.getKeywordId(), event.getProductId(), e);
            throw e;
        }
    }

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "keyword.deleted",
                    description = "검색 키워드 삭제 이벤트 구독 - Elasticsearch 상품 문서에서 키워드 제거",
                    message = @AsyncMessage(
                            messageId = "keywordDeletedEvent",
                            name = "KeywordDeletedEvent"
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
    @KafkaListener(topics = "keyword.deleted", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeKeywordDeletedEvent(
            @Payload KeywordDeletedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received keyword.deleted event: keywordId={}, productId={}, keyword={}, topic={}, offset={}",
                event.getKeywordId(), event.getProductId(), event.getKeyword(), topic, offset);

        try {
            keywordSyncService.removeKeyword(event);
            log.info("Successfully processed keyword.deleted event: keywordId={}", event.getKeywordId());
        } catch (Exception e) {
            log.error("Failed to process keyword.deleted event: keywordId={}, productId={}",
                    event.getKeywordId(), event.getProductId(), e);
            throw e;
        }
    }

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

        if (payload instanceof KeywordCreatedEvent event) {
            log.error("DLQ 처리 필요 - keyword.created 실패: keywordId={}, productId={}, keyword={}",
                    event.getKeywordId(), event.getProductId(), event.getKeyword());
        } else if (payload instanceof KeywordDeletedEvent event) {
            log.error("DLQ 처리 필요 - keyword.deleted 실패: keywordId={}, productId={}, keyword={}",
                    event.getKeywordId(), event.getProductId(), event.getKeyword());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import com.example.catalogservice.service.ProductSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Product 이벤트 컨슈머
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ(Dead Letter Queue)로 전송
 *
 * 멱등성:
 * - Elasticsearch는 document ID 기반 upsert로 동작하므로
 *   같은 productId로 여러 번 저장해도 결과가 동일함 (자연적 멱등성)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductSyncService productSyncService;
    private final ObjectMapper objectMapper;

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "product.created",
                    description = "상품 등록 이벤트 구독 - Elasticsearch에 신규 상품 인덱싱",
                    message = @AsyncMessage(
                            messageId = "productCreatedEvent",
                            name = "ProductCreatedEvent"
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
    @KafkaListener(topics = "product.created", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeProductCreatedEvent(
            @Payload ProductCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received product.created event: productId={}, topic={}, offset={}",
                event.getProductId(), topic, offset);

        try {
            productSyncService.indexProduct(event);
            log.info("Successfully processed product.created event: productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product.created event: productId={}", event.getProductId(), e);
            throw e;
        }
    }

    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "product.updated",
                    description = "상품 수정 이벤트 구독 - Elasticsearch에 상품 정보 갱신",
                    message = @AsyncMessage(
                            messageId = "productUpdatedEvent",
                            name = "ProductUpdatedEvent"
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
    @KafkaListener(topics = "product.updated", groupId = "${spring.kafka.consumer.group-id:catalog-service}")
    public void consumeProductUpdatedEvent(
            @Payload ProductUpdatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received product.updated event: productId={}, topic={}, offset={}",
                event.getProductId(), topic, offset);

        try {
            productSyncService.updateProduct(event);
            log.info("Successfully processed product.updated event: productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("Failed to process product.updated event: productId={}", event.getProductId(), e);
            throw e;
        }
    }

    /**
     * DLQ(Dead Letter Queue) 핸들러
     * 모든 재시도가 실패한 후 호출됩니다.
     *
     * Elasticsearch 저장 실패 원인:
     * - ES 클러스터 장애
     * - 네트워크 이슈
     * - 잘못된 데이터 형식
     *
     * DLQ 메시지는 Kafka UI 또는 모니터링 도구를 통해 확인하고
     * 수동으로 재처리할 수 있습니다.
     */
    @DltHandler
    public void handleDlt(
            @Payload String message,
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
                Message: {}
                Exception: {}
                ========================================
                """, topic, originalTopic, offset, message, exceptionMessage);

        // 이벤트 타입 파악을 위한 파싱 시도
        try {
            if (originalTopic != null && originalTopic.contains("product.created")) {
                ProductCreatedEvent event = objectMapper.readValue(message, ProductCreatedEvent.class);
                log.error("DLQ 처리 필요 - product.created 실패: productId={}, productName={}",
                        event.getProductId(), event.getProductName());
            } else if (originalTopic != null && originalTopic.contains("product.updated")) {
                ProductUpdatedEvent event = objectMapper.readValue(message, ProductUpdatedEvent.class);
                log.error("DLQ 처리 필요 - product.updated 실패: productId={}, productName={}",
                        event.getProductId(), event.getProductName());
            }
        } catch (Exception e) {
            log.error("DLQ 메시지 파싱 실패: {}", message, e);
        }

        // 참고: RDBMS가 없으므로 DB 저장 대신 로그로 기록
        // 운영 환경에서는 Slack/Email 알림 또는 외부 모니터링 시스템 연동 권장
    }
}

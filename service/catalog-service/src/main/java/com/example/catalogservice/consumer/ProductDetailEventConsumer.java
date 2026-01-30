package com.example.catalogservice.consumer;

import com.example.catalogservice.consumer.event.ProductCreatedEvent;
import com.example.catalogservice.consumer.event.ProductUpdatedEvent;
import com.example.catalogservice.service.ProductDetailService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 상품 상세 캐시용 이벤트 컨슈머
 *
 * 기존 ProductEventConsumer(상품 목록용, ES 인덱싱)와 별도의 consumer group으로 동작하여
 * 같은 토픽(product.created, product.updated)을 독립적으로 구독한다.
 *
 * Data Enrichment 패턴:
 * - 이벤트에는 최소한의 정보(productId)만 사용
 * - product-service 상세 API를 호출하여 전체 데이터를 가져온 뒤 Redis에 캐싱
 *
 * 재시도 전략:
 * - 총 4회 시도 (원본 1회 + 재시도 3회)
 * - 지수 백오프: 1초 -> 2초 -> 4초
 * - 모든 재시도 실패 시 DLQ로 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDetailEventConsumer {

    private static final String GROUP_ID = "${spring.kafka.consumer.group-id:catalog-service}-detail";

    private final ProductDetailService productDetailService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-detail-retry",
            dltTopicSuffix = "-detail-dlt"
    )
    @KafkaListener(topics = "product.created", groupId = GROUP_ID)
    public void consumeProductCreatedEvent(
            @Payload ProductCreatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("[Detail] Received product.created event: productId={}, topic={}, offset={}",
                event.getProductId(), topic, offset);

        try {
            productDetailService.refreshCache(event.getProductId());
            log.info("[Detail] Successfully cached product.created: productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("[Detail] Failed to cache product.created: productId={}", event.getProductId(), e);
            throw e;
        }
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 1000,
                    multiplier = 2.0,
                    maxDelay = 10000
            ),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-detail-retry",
            dltTopicSuffix = "-detail-dlt"
    )
    @KafkaListener(topics = "product.updated", groupId = GROUP_ID)
    public void consumeProductUpdatedEvent(
            @Payload ProductUpdatedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("[Detail] Received product.updated event: productId={}, topic={}, offset={}",
                event.getProductId(), topic, offset);

        try {
            productDetailService.refreshCache(event.getProductId());
            log.info("[Detail] Successfully cached product.updated: productId={}", event.getProductId());
        } catch (Exception e) {
            log.error("[Detail] Failed to cache product.updated: productId={}", event.getProductId(), e);
            throw e;
        }
    }

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
                [Detail] DLQ 메시지 수신 (재시도 실패)
                ========================================
                DLT Topic: {}
                Original Topic: {}
                Offset: {}
                Message: {}
                Exception: {}
                ========================================
                """, topic, originalTopic, offset, message, exceptionMessage);

        try {
            if (originalTopic != null && originalTopic.contains("product.created")) {
                ProductCreatedEvent event = objectMapper.readValue(message, ProductCreatedEvent.class);
                log.error("[Detail] DLQ 처리 필요 - product.created 캐시 실패: productId={}", event.getProductId());
            } else if (originalTopic != null && originalTopic.contains("product.updated")) {
                ProductUpdatedEvent event = objectMapper.readValue(message, ProductUpdatedEvent.class);
                log.error("[Detail] DLQ 처리 필요 - product.updated 캐시 실패: productId={}", event.getProductId());
            }
        } catch (Exception e) {
            log.error("[Detail] DLQ 메시지 파싱 실패: {}", message, e);
        }
    }
}

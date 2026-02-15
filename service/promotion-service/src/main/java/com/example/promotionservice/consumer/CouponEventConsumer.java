package com.example.promotionservice.consumer;

import com.example.promotionservice.consumer.event.CouponRestoredEvent;
import com.example.promotionservice.consumer.event.CouponUsedEvent;
import com.example.promotionservice.domain.entity.UserCoupon;
import com.example.promotionservice.domain.entity.UserCouponStatus;
import com.example.promotionservice.repository.CouponRepository;
import com.example.promotionservice.repository.DiscountPolicyRepository;
import com.example.promotionservice.repository.UserCouponRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventConsumer {

    private final CouponRepository couponRepository;
    private final DiscountPolicyRepository discountPolicyRepository;
    private final UserCouponRepository userCouponRepository;

    /**
     * 쿠폰 사용 이벤트 구독
     */
    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "coupon.used",
                    description = "쿠폰 사용 이벤트 구독 - 사용자 쿠폰 상태를 USED로 변경",
                    message = @AsyncMessage(
                            messageId = "couponUsedEvent",
                            name = "CouponUsedEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-promotion-retry",
            dltTopicSuffix = "-promotion-dlt"
    )
    @KafkaListener(
            topics = "coupon.used",
            groupId = "${spring.kafka.consumer.group-id:promotion-service}"
    )
    @Transactional
    public void consumeCouponUsedEvent(
            @Payload CouponUsedEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received coupon.used event: orderId={}, userCouponId={}, topic={}, offset={}",
                event.getOrderId(), event.getUserCouponId(), topic, offset);

        try {
            UserCoupon userCoupon = userCouponRepository.findById(event.getUserCouponId())
                    .orElse(null);

            if (userCoupon == null) {
                log.warn("UserCoupon not found: userCouponId={}", event.getUserCouponId());
                return;
            }

            // 멱등성 보장
            if (userCoupon.getCouponStatus() == UserCouponStatus.USED) {
                log.info("UserCoupon already USED, skipping: userCouponId={}",
                        event.getUserCouponId());
                return;
            }

            userCoupon.use();
            userCouponRepository.save(userCoupon);

            log.info("Successfully marked UserCoupon as USED: userCouponId={}",
                    event.getUserCouponId());

        } catch (Exception e) {
            log.error("Failed to process coupon.used event: userCouponId={}",
                    event.getUserCouponId(), e);
            throw e;
        }
    }

    /**
     * 쿠폰 복구 이벤트 구독
     */
    @AsyncListener(
            operation = @AsyncOperation(
                    channelName = "coupon.restored",
                    description = "쿠폰 복구 이벤트 구독 - 사용자 쿠폰 상태를 RESTORED로 변경",
                    message = @AsyncMessage(
                            messageId = "couponRestoredEvent",
                            name = "CouponRestoredEvent"
                    )
            )
    )
    @KafkaAsyncOperationBinding
    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopics = "false",
            include = {Exception.class},
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            retryTopicSuffix = "-promotion-retry",
            dltTopicSuffix = "-promotion-dlt"
    )
    @KafkaListener(
            topics = "coupon.restored",
            groupId = "${spring.kafka.consumer.group-id:promotion-service}"
    )
    @Transactional
    public void consumeCouponRestoredEvent(
            @Payload CouponRestoredEvent event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset
    ) {
        log.info("Received coupon.restored event: orderId={}, userCouponId={}, topic={}, offset={}",
                event.getOrderId(), event.getUserCouponId(), topic, offset);

        try {
            UserCoupon userCoupon = userCouponRepository.findById(event.getUserCouponId())
                    .orElse(null);

            if (userCoupon == null) {
                log.warn("UserCoupon not found: userCouponId={}", event.getUserCouponId());
                return;
            }

            // 멱등성 보장
            if (userCoupon.getCouponStatus() == UserCouponStatus.RESTORED) {
                log.info("UserCoupon already RESTORED, skipping: userCouponId={}",
                        event.getUserCouponId());
                return;
            }

            userCoupon.restore();
            userCouponRepository.save(userCoupon);

            log.info("Successfully restored UserCoupon: userCouponId={}",
                    event.getUserCouponId());

        } catch (Exception e) {
            log.error("Failed to process coupon.restored event: userCouponId={}",
                    event.getUserCouponId(), e);
            throw e;
        }
    }

    /**
     * DLQ 핸들러
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

        if (payload instanceof CouponUsedEvent event) {
            log.error("DLQ 처리 필요 - coupon.used 실패: userCouponId={}, orderId={}",
                    event.getUserCouponId(), event.getOrderId());
        } else if (payload instanceof CouponRestoredEvent event) {
            log.error("DLQ 처리 필요 - coupon.restored 실패: userCouponId={}, orderId={}",
                    event.getUserCouponId(), event.getOrderId());
        } else {
            log.error("DLQ 알 수 없는 payload 타입: {}", payload.getClass().getName());
        }
    }
}

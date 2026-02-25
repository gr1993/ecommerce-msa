package com.example.shippingservice.returns.scheduler;

import com.example.shippingservice.client.dto.TrackingDetail;
import com.example.shippingservice.client.dto.TrackingInfoResponse;
import com.example.shippingservice.domain.entity.Outbox;
import com.example.shippingservice.domain.event.ReturnCompletedEvent;
import com.example.shippingservice.domain.event.ReturnInTransitEvent;
import com.example.shippingservice.global.common.EventTypeConstants;
import com.example.shippingservice.repository.OutboxRepository;
import com.example.shippingservice.returns.entity.OrderReturn;
import com.example.shippingservice.returns.enums.ReturnStatus;
import com.example.shippingservice.returns.repository.OrderReturnHistoryRepository;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
import com.example.shippingservice.shipping.enums.CarrierCode;
import com.example.shippingservice.shipping.service.MockDeliveryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnTrackingScheduler {

    private final OrderReturnRepository orderReturnRepository;
    private final OrderReturnHistoryRepository orderReturnHistoryRepository;
    private final MockDeliveryService mockDeliveryService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedRateString = "${shipping.tracking.poll-interval-ms:60000}")
    @Transactional
    public void pollDeliveryStatuses() {
        List<OrderReturn> targets = orderReturnRepository.findByReturnStatusIn(
                List.of(ReturnStatus.RETURN_APPROVED, ReturnStatus.RETURN_IN_TRANSIT)
        );

        if (targets.isEmpty()) {
            log.debug("반품 추적 폴링: 추적 대상 없음");
            return;
        }

        log.info("반품 추적 폴링 시작 - 대상: {}건", targets.size());

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (OrderReturn orderReturn : targets) {
            try {
                boolean changed = pollAndUpdate(orderReturn);
                if (changed) updated++;
                else skipped++;
            } catch (Exception e) {
                log.error("반품 상태 업데이트 실패 - returnId: {}, trackingNumber: {}",
                        orderReturn.getReturnId(), orderReturn.getTrackingNumber(), e);
                failed++;
            }
        }

        log.info("반품 추적 폴링 완료 - 업데이트: {}건, 변경없음: {}건, 실패: {}건", updated, skipped, failed);
    }

    private boolean pollAndUpdate(OrderReturn orderReturn) {
        String courierCode = resolveCarrierCode(orderReturn.getCourier());

        TrackingInfoResponse response = (courierCode != null)
                ? mockDeliveryService.getTrackingInfo(courierCode, orderReturn.getTrackingNumber())
                : mockDeliveryService.getTrackingInfo(orderReturn.getTrackingNumber());

        if (response == null || response.getLastDetail() == null) {
            log.warn("배송 조회 응답 없음 - returnId: {}, trackingNumber: {}",
                    orderReturn.getReturnId(), orderReturn.getTrackingNumber());
            return false;
        }

        TrackingDetail lastDetail = response.getLastDetail();
        String currentKind = lastDetail.getKind();

        if (currentKind == null) {
            log.debug("배송 상태 kind 없음 - returnId: {}", orderReturn.getReturnId());
            return false;
        }

        // 마지막으로 기록된 외부 택배사 상태(kind) 조회
        Optional<String> lastTrackingKind = orderReturnHistoryRepository
                .findLastTrackingKindByReturnId(orderReturn.getReturnId());

        // 외부 택배사 상태가 동일하면 업데이트 불필요
        if (lastTrackingKind.isPresent() && lastTrackingKind.get().equals(currentKind)) {
            log.debug("외부 택배사 상태 변경 없음 - returnId: {}, kind: {}",
                    orderReturn.getReturnId(), currentKind);
            return false;
        }

        ReturnStatus previousStatus = orderReturn.getReturnStatus();
        ReturnStatus mappedStatus = mapToInternalStatus(currentKind);

        if (mappedStatus != null) {
            // 상태 업데이트 및 히스토리 추가
            orderReturn.updateReturnStatus(mappedStatus);
            orderReturn.addReturnHistory(
                    previousStatus,
                    mappedStatus,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("반품 상태 업데이트 - returnId: {}, orderId: {}, "
                            + "returnStatus: {} → {}, kind: {}",
                    orderReturn.getReturnId(), orderReturn.getOrderId(),
                    previousStatus, mappedStatus, currentKind);

            // 이벤트 발행
            if (previousStatus != ReturnStatus.RETURN_IN_TRANSIT
                    && mappedStatus == ReturnStatus.RETURN_IN_TRANSIT) {
                saveReturnInTransitOutbox(orderReturn);
            } else if (previousStatus != ReturnStatus.RETURNED
                    && mappedStatus == ReturnStatus.RETURNED) {
                saveReturnCompletedOutbox(orderReturn);
            }
        } else {
            // 내부 상태 변경 없이 이력만 추가
            orderReturn.addReturnHistory(
                    previousStatus,
                    previousStatus,
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );

            log.info("반품 이력 추가 (상태 변경 없음) - returnId: {}, orderId: {}, kind: {}",
                    orderReturn.getReturnId(), orderReturn.getOrderId(), currentKind);
        }

        return true;
    }

    private ReturnStatus mapToInternalStatus(String kind) {
        if (kind == null) return null;
        return switch (kind) {
            case "IN_TRANSIT", "AT_DESTINATION", "OUT_FOR_DELIVERY" -> ReturnStatus.RETURN_IN_TRANSIT;
            case "DELIVERED" -> ReturnStatus.RETURNED;
            default -> null; // ACCEPTED, PICKED_UP
        };
    }

    private String resolveCarrierCode(String shippingCompany) {
        if (shippingCompany == null || shippingCompany.isBlank()) {
            return null;
        }
        try {
            return CarrierCode.fromName(shippingCompany).getCode();
        } catch (IllegalArgumentException e) {
            log.warn("택배사 코드 조회 실패, 기본값 사용 - shippingCompany: {}", shippingCompany);
            return null;
        }
    }

    private void saveReturnInTransitOutbox(OrderReturn orderReturn) {
        ReturnInTransitEvent event = ReturnInTransitEvent.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .userId(orderReturn.getUserId())
                .courier(orderReturn.getCourier())
                .trackingNumber(orderReturn.getTrackingNumber())
                .inTransitAt(LocalDateTime.now())
                .build();

        saveOutbox(orderReturn.getReturnId(), EventTypeConstants.TOPIC_RETURN_IN_TRANSIT, event);
    }

    private void saveReturnCompletedOutbox(OrderReturn orderReturn) {
        ReturnCompletedEvent event = ReturnCompletedEvent.builder()
                .returnId(orderReturn.getReturnId())
                .orderId(orderReturn.getOrderId())
                .userId(orderReturn.getUserId())
                .reason(orderReturn.getReason())
                .completedAt(LocalDateTime.now())
                .build();

        saveOutbox(orderReturn.getReturnId(), EventTypeConstants.TOPIC_RETURN_COMPLETED, event);
    }

    private void saveOutbox(Long returnId, String eventType, Object eventPayload) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Return")
                    .aggregateId(String.valueOf(returnId))
                    .eventType(eventType)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("Outbox 이벤트 저장 완료: returnId={}, eventType={}", returnId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 직렬화 실패: returnId={}, eventType={}", returnId, eventType, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}

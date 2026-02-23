package com.example.shippingservice.shipping.scheduler;

import com.example.shippingservice.client.dto.TrackingDetail;
import com.example.shippingservice.client.dto.TrackingInfoResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.CarrierCode;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingHistoryRepository;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import com.example.shippingservice.shipping.service.MockDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 배송 상태 추적 스케줄러
 *
 * deliveryServiceStatus 가 SENT 또는 IN_TRANSIT 인 배송 건을 대상으로
 * 외부 택배사 API를 주기적으로 폴링하여 배송 상태를 최신 상태로 동기화합니다.
 *
 * 외부 택배사 상태(kind)가 변경될 때마다 이력을 저장하며,
 * 내부 상태 매핑이 필요한 경우에만 내부 상태를 업데이트합니다.
 *
 * 외부 API kind 값 → 내부 상태 매핑:
 * - ACCEPTED, PICKED_UP               → 이력만 추가 (내부 상태 변경 없음)
 * - IN_TRANSIT, AT_DESTINATION,
 *   OUT_FOR_DELIVERY                  → IN_TRANSIT / SHIPPING
 * - DELIVERED                         → DELIVERED  / DELIVERED
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingTrackingScheduler {

    private final OrderShippingRepository orderShippingRepository;
    private final OrderShippingHistoryRepository orderShippingHistoryRepository;
    private final MockDeliveryService mockDeliveryService;

    @Scheduled(fixedRateString = "${shipping.tracking.poll-interval-ms:60000}")
    @Transactional
    public void pollDeliveryStatuses() {
        List<OrderShipping> targets = orderShippingRepository.findByDeliveryServiceStatusIn(
                List.of(DeliveryServiceStatus.SENT, DeliveryServiceStatus.IN_TRANSIT)
        );

        if (targets.isEmpty()) {
            log.debug("배송 추적 폴링: 추적 대상 없음");
            return;
        }

        log.info("배송 추적 폴링 시작 - 대상: {}건", targets.size());

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (OrderShipping shipping : targets) {
            try {
                boolean changed = pollAndUpdate(shipping);
                if (changed) updated++;
                else skipped++;
            } catch (Exception e) {
                log.error("배송 상태 업데이트 실패 - shippingId: {}, trackingNumber: {}",
                        shipping.getShippingId(), shipping.getTrackingNumber(), e);
                failed++;
            }
        }

        log.info("배송 추적 폴링 완료 - 업데이트: {}건, 변경없음: {}건, 실패: {}건", updated, skipped, failed);
    }

    private boolean pollAndUpdate(OrderShipping shipping) {
        String courierCode = resolveCarrierCode(shipping.getShippingCompany());

        TrackingInfoResponse response = (courierCode != null)
                ? mockDeliveryService.getTrackingInfo(courierCode, shipping.getTrackingNumber())
                : mockDeliveryService.getTrackingInfo(shipping.getTrackingNumber());

        if (response == null || response.getLastDetail() == null) {
            log.warn("배송 조회 응답 없음 - shippingId: {}, trackingNumber: {}",
                    shipping.getShippingId(), shipping.getTrackingNumber());
            return false;
        }

        TrackingDetail lastDetail = response.getLastDetail();
        String currentKind = lastDetail.getKind();

        if (currentKind == null) {
            log.debug("배송 상태 kind 없음 - shippingId: {}", shipping.getShippingId());
            return false;
        }

        // 마지막으로 기록된 외부 택배사 상태(kind) 조회
        Optional<String> lastTrackingKind = orderShippingHistoryRepository
                .findLastTrackingKindByShippingId(shipping.getShippingId());

        // 외부 택배사 상태가 동일하면 업데이트 불필요
        if (lastTrackingKind.isPresent() && lastTrackingKind.get().equals(currentKind)) {
            log.debug("외부 택배사 상태 변경 없음 - shippingId: {}, kind: {}",
                    shipping.getShippingId(), currentKind);
            return false;
        }

        // 내부 상태 매핑 (null이면 내부 상태 변경 없이 이력만 추가)
        StatusMapping mapping = mapToInternalStatus(currentKind);

        ShippingStatus previousShippingStatus = shipping.getShippingStatus();
        DeliveryServiceStatus previousDeliveryStatus = shipping.getDeliveryServiceStatus();

        if (mapping != null) {
            // 내부 상태 변경이 필요한 경우
            shipping.updateShippingStatusWithDetail(
                    mapping.shippingStatus(),
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind,
                    "DELIVERY_API"
            );
            shipping.updateDeliveryServiceStatus(mapping.deliveryServiceStatus());

            log.info("배송 상태 업데이트 - shippingId: {}, orderNumber: {}, "
                            + "shippingStatus: {} → {}, deliveryServiceStatus: {} → {}, kind: {}",
                    shipping.getShippingId(), shipping.getOrderNumber(),
                    previousShippingStatus, mapping.shippingStatus(),
                    previousDeliveryStatus, mapping.deliveryServiceStatus(),
                    currentKind);
        } else {
            // 내부 상태 변경 없이 이력만 추가 (ACCEPTED, PICKED_UP 등)
            shipping.addTrackingDetail(
                    lastDetail.getWhere(),
                    lastDetail.getRemark(),
                    currentKind
            );

            log.info("배송 이력 추가 (상태 변경 없음) - shippingId: {}, orderNumber: {}, kind: {}",
                    shipping.getShippingId(), shipping.getOrderNumber(), currentKind);
        }

        return true;
    }

    /**
     * 외부 택배사 API kind 값을 내부 상태로 매핑합니다.
     * ACCEPTED, PICKED_UP 은 송장 발급 후 수거 단계로, 상태 변경 없음(null 반환).
     */
    private StatusMapping mapToInternalStatus(String kind) {
        if (kind == null) return null;
        return switch (kind) {
            case "IN_TRANSIT", "AT_DESTINATION", "OUT_FOR_DELIVERY" ->
                    new StatusMapping(DeliveryServiceStatus.IN_TRANSIT, ShippingStatus.SHIPPING);
            case "DELIVERED" ->
                    new StatusMapping(DeliveryServiceStatus.DELIVERED, ShippingStatus.DELIVERED);
            default -> null; // ACCEPTED, PICKED_UP, CANCELLED 등
        };
    }

    /**
     * 저장된 택배사 이름으로 택배사 코드를 조회합니다.
     * 조회 실패 시 기본 택배사 코드(defaultCourierCode)를 사용하도록 null을 반환합니다.
     */
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

    private record StatusMapping(DeliveryServiceStatus deliveryServiceStatus, ShippingStatus shippingStatus) {}
}

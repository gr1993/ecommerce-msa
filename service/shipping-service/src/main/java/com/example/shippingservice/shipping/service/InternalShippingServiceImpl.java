package com.example.shippingservice.shipping.service;

import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;
import com.example.shippingservice.shipping.entity.OrderShipping;
import com.example.shippingservice.shipping.enums.DeliveryServiceStatus;
import com.example.shippingservice.shipping.enums.ShippingStatus;
import com.example.shippingservice.shipping.repository.OrderShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalShippingServiceImpl implements InternalShippingService {

    private final OrderShippingRepository orderShippingRepository;

    @Override
    @Transactional(readOnly = true)
    public ShippingCancellableResponse checkCancellable(Long orderId) {
        Optional<OrderShipping> optShipping = orderShippingRepository.findByOrderId(orderId);

        if (optShipping.isEmpty()) {
            // order.created 이벤트 아직 미처리 등의 상황 - 배송 기록 없으면 취소 가능
            log.info("배송 레코드 없음 - 취소 가능으로 판단: orderId={}", orderId);
            return ShippingCancellableResponse.notFound();
        }

        OrderShipping shipping = optShipping.get();
        ShippingStatus shippingStatus = shipping.getShippingStatus();
        DeliveryServiceStatus deliveryStatus = shipping.getDeliveryServiceStatus();

        if (shippingStatus == ShippingStatus.CANCELLED) {
            return ShippingCancellableResponse.notCancellable(shippingStatus, "이미 취소된 배송입니다.");
        }

        // IN_TRANSIT 이상이면 실제 배송이 시작된 것이므로 취소 불가
        if (deliveryStatus == DeliveryServiceStatus.IN_TRANSIT
                || deliveryStatus == DeliveryServiceStatus.DELIVERED) {
            log.info("배송 취소 불가 - orderId={}, deliveryServiceStatus={}", orderId, deliveryStatus);
            return ShippingCancellableResponse.notCancellable(
                    shippingStatus,
                    "이미 배송이 진행 중입니다. 현재 배송사 상태: " + deliveryStatus
            );
        }

        log.info("배송 취소 가능 - orderId={}, shippingStatus={}, deliveryServiceStatus={}",
                orderId, shippingStatus, deliveryStatus);
        return ShippingCancellableResponse.cancellable(shippingStatus);
    }
}

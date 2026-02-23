package com.example.shippingservice.shipping.service;

import com.example.shippingservice.exchange.repository.OrderExchangeRepository;
import com.example.shippingservice.returns.repository.OrderReturnRepository;
import com.example.shippingservice.shipping.dto.request.TestCreateShippingRequest;
import com.example.shippingservice.shipping.dto.response.ShippingCancellableResponse;
import com.example.shippingservice.shipping.dto.response.TestCreateShippingResponse;
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
    private final OrderReturnRepository orderReturnRepository;
    private final OrderExchangeRepository orderExchangeRepository;

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

    @Override
    @Transactional
    public TestCreateShippingResponse createShippingForTest(TestCreateShippingRequest request) {
        log.info("[TEST] 배송 정보 생성 요청: orderId={}, orderNumber={}",
                request.getOrderId(), request.getOrderNumber());

        // 멱등성 보장: 이미 존재하는 경우 기존 데이터 반환
        Optional<OrderShipping> existing = orderShippingRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            log.info("[TEST] 이미 존재하는 배송 정보 반환: orderId={}", request.getOrderId());
            return TestCreateShippingResponse.from(existing.get());
        }

        TestCreateShippingRequest.DeliveryInfo delivery = request.getDelivery();
        String fullAddress = buildFullAddress(delivery);

        OrderShipping orderShipping = OrderShipping.builder()
                .orderId(request.getOrderId())
                .orderNumber(request.getOrderNumber())
                .userId(request.getUserId())
                .receiverName(delivery.getReceiverName())
                .receiverPhone(delivery.getReceiverPhone())
                .address(fullAddress)
                .postalCode(delivery.getZipcode())
                .shippingStatus(ShippingStatus.READY)
                .deliveryServiceStatus(DeliveryServiceStatus.NOT_SENT)
                .build();

        OrderShipping saved = orderShippingRepository.save(orderShipping);
        log.info("[TEST] 배송 정보 생성 완료: shippingId={}, orderId={}",
                saved.getShippingId(), saved.getOrderId());

        return TestCreateShippingResponse.from(saved);
    }

    private String buildFullAddress(TestCreateShippingRequest.DeliveryInfo delivery) {
        if (delivery.getAddressDetail() == null || delivery.getAddressDetail().isBlank()) {
            return delivery.getAddress();
        }
        return delivery.getAddress() + " " + delivery.getAddressDetail();
    }

    @Override
    @Transactional
    public void deleteShippingForTest(Long orderId) {
        log.info("[TEST] 테스트 데이터 삭제 요청: orderId={}", orderId);

        // 반품 정보 삭제
        orderReturnRepository.findByOrderId(orderId).ifPresent(orderReturn -> {
            orderReturnRepository.delete(orderReturn);
            log.info("[TEST] 반품 정보 삭제 완료: orderId={}, returnId={}", orderId, orderReturn.getReturnId());
        });

        // 교환 정보 삭제
        orderExchangeRepository.findByOrderId(orderId).ifPresent(orderExchange -> {
            orderExchangeRepository.delete(orderExchange);
            log.info("[TEST] 교환 정보 삭제 완료: orderId={}, exchangeId={}", orderId, orderExchange.getExchangeId());
        });

        // 배송 정보 삭제
        orderShippingRepository.findByOrderId(orderId).ifPresent(orderShipping -> {
            orderShippingRepository.delete(orderShipping);
            log.info("[TEST] 배송 정보 삭제 완료: orderId={}, shippingId={}", orderId, orderShipping.getShippingId());
        });

        log.info("[TEST] 테스트 데이터 삭제 완료: orderId={}", orderId);
    }
}

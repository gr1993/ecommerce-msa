package com.example.orderservice.service;

import com.example.orderservice.client.ShippingServiceClient;
import com.example.orderservice.client.dto.CreateExchangeRequest;
import com.example.orderservice.client.dto.CreateExchangeResponse;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.response.ExchangeOrderResponse;
import com.example.orderservice.global.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExchangeService {

    private final OrderRepository orderRepository;
    private final ShippingServiceClient shippingServiceClient;

    @Transactional(readOnly = true)
    public ExchangeOrderResponse requestExchange(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 주문 소유권 검증
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 주문만 교환 신청할 수 있습니다.");
        }

        // 주문 상태 검증 (DELIVERED만 가능)
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "배송 완료 상태에서만 교환 신청이 가능합니다. 현재 상태: " + order.getOrderStatus());
        }

        // shipping-service에 교환 생성 요청 (Feign)
        CreateExchangeRequest request = CreateExchangeRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .reason(reason)
                .build();

        try {
            CreateExchangeResponse exchangeResponse = shippingServiceClient.createExchange(request);

            log.info("교환 신청 완료 - orderId={}, exchangeId={}", orderId, exchangeResponse.getExchangeId());

            return ExchangeOrderResponse.builder()
                    .exchangeId(exchangeResponse.getExchangeId())
                    .orderId(orderId)
                    .orderNumber(order.getOrderNumber())
                    .exchangeStatus(exchangeResponse.getExchangeStatus())
                    .reason(exchangeResponse.getReason())
                    .requestedAt(exchangeResponse.getRequestedAt())
                    .build();
        } catch (feign.FeignException.BadRequest e) {
            log.warn("교환 신청 실패 (Bad Request) - orderId={}: {}", orderId, e.getMessage());
            throw new IllegalArgumentException("교환 신청에 실패했습니다. 배송 정보를 확인해 주세요.");
        } catch (feign.FeignException.Conflict e) {
            log.warn("교환 신청 실패 (Conflict) - orderId={}: {}", orderId, e.getMessage());
            throw new IllegalStateException("교환 신청이 불가합니다. 이미 진행 중인 반품 또는 교환 건이 있습니다.");
        } catch (Exception e) {
            log.error("교환 신청 중 오류 발생 - orderId={}", orderId, e);
            throw new RuntimeException("교환 신청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}

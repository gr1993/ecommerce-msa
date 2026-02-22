package com.example.orderservice.service;

import com.example.orderservice.client.ShippingServiceClient;
import com.example.orderservice.client.dto.CreateReturnRequest;
import com.example.orderservice.client.dto.CreateReturnResponse;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.response.ReturnOrderResponse;
import com.example.orderservice.global.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReturnService {

    private final OrderRepository orderRepository;
    private final ShippingServiceClient shippingServiceClient;

    @Transactional
    public ReturnOrderResponse requestReturn(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 주문 소유권 검증
        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인 주문만 반품 신청할 수 있습니다.");
        }

        // 주문 상태 검증 (DELIVERED만 가능)
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "배송 완료 상태에서만 반품 신청이 가능합니다. 현재 상태: " + order.getOrderStatus());
        }

        // shipping-service에 반품 생성 요청 (Feign)
        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .reason(reason)
                .build();

        try {
            CreateReturnResponse returnResponse = shippingServiceClient.createReturn(request);

            order.updateStatus(OrderStatus.RETURN_REQUESTED);

            log.info("반품 신청 완료 - orderId={}, returnId={}", orderId, returnResponse.getReturnId());

            return ReturnOrderResponse.builder()
                    .returnId(returnResponse.getReturnId())
                    .orderId(orderId)
                    .orderNumber(order.getOrderNumber())
                    .returnStatus(returnResponse.getReturnStatus())
                    .reason(returnResponse.getReason())
                    .requestedAt(returnResponse.getRequestedAt())
                    .build();
        } catch (feign.FeignException.BadRequest e) {
            log.warn("반품 신청 실패 (Bad Request) - orderId={}: {}", orderId, e.getMessage());
            throw new IllegalArgumentException("반품 신청에 실패했습니다. 배송 정보를 확인해 주세요.");
        } catch (feign.FeignException.Conflict e) {
            log.warn("반품 신청 실패 (Conflict) - orderId={}: {}", orderId, e.getMessage());
            throw new IllegalStateException("반품 신청이 불가합니다. 이미 진행 중인 반품 또는 교환 건이 있습니다.");
        } catch (Exception e) {
            log.error("반품 신청 중 오류 발생 - orderId={}", orderId, e);
            throw new RuntimeException("반품 신청 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }
}

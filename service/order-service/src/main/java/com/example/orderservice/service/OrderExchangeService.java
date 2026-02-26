package com.example.orderservice.service;

import com.example.orderservice.client.ShippingServiceClient;
import com.example.orderservice.client.dto.CreateExchangeRequest;
import com.example.orderservice.client.dto.CreateExchangeResponse;
import com.example.orderservice.client.dto.ExchangeItemDto;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.ExchangeItemRequest;
import com.example.orderservice.dto.response.ExchangeOrderResponse;
import com.example.orderservice.global.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExchangeService {

    private final OrderRepository orderRepository;
    private final ShippingServiceClient shippingServiceClient;
    private final com.example.orderservice.repository.OrderItemRepository orderItemRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public ExchangeOrderResponse requestExchange(Long userId, Long orderId, List<ExchangeItemRequest> exchangeItemRequests, String reason) {
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

        // 교환 상품 목록 검증 및 DTO 변환
        List<ExchangeItemDto> exchangeItems = new ArrayList<>();
        for (ExchangeItemRequest itemRequest : exchangeItemRequests) {
            Long orderItemId = itemRequest.getOrderItemId();
            Long newSkuId = itemRequest.getNewSkuId();
            Integer quantity = itemRequest.getQuantity() != null ? itemRequest.getQuantity() : 1;

            // 주문 상품 조회 및 검증
            com.example.orderservice.domain.entity.OrderItem orderItem = orderItemRepository.findById(orderItemId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 상품을 찾을 수 없습니다. orderItemId=" + orderItemId));

            // 주문 상품이 해당 주문에 속하는지 검증
            if (!orderItem.getOrder().getId().equals(orderId)) {
                throw new IllegalArgumentException("해당 주문에 속하지 않은 주문 상품입니다. orderItemId=" + orderItemId);
            }

            Long originalSkuId = orderItem.getSkuId();
            Long productId = orderItem.getProductId();

            // 동일 상품인지 검증 (product-service 호출)
            validateSameProduct(productId, originalSkuId, newSkuId);

            // ExchangeItemDto 생성
            exchangeItems.add(ExchangeItemDto.builder()
                    .orderItemId(orderItemId)
                    .originalOptionId(originalSkuId)
                    .newOptionId(newSkuId)
                    .quantity(quantity)
                    .build());
        }

        // shipping-service에 교환 생성 요청 (Feign)
        CreateExchangeRequest request = CreateExchangeRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .exchangeItems(exchangeItems)
                .reason(reason)
                .build();

        try {
            CreateExchangeResponse exchangeResponse = shippingServiceClient.createExchange(request);

            order.updateStatus(OrderStatus.EXCHANGE_REQUESTED);

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

    /**
     * 동일 상품인지 검증 (originalSkuId와 newSkuId가 같은 productId에 속하는지)
     */
    private void validateSameProduct(Long productId, Long originalSkuId, Long newSkuId) {
        try {
            com.example.orderservice.client.dto.ProductDetailResponse productDetail =
                    productServiceClient.getProductDetail(productId);

            if (productDetail == null || productDetail.getSkus() == null) {
                throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다. productId=" + productId);
            }

            List<Long> skuIds = productDetail.getSkus().stream()
                    .map(com.example.orderservice.client.dto.ProductDetailResponse.SkuResponse::getId)
                    .toList();

            // originalSkuId 검증
            if (!skuIds.contains(originalSkuId)) {
                throw new IllegalArgumentException("유효하지 않은 기존 옵션입니다. skuId=" + originalSkuId);
            }

            // newSkuId 검증 (동일 상품의 다른 옵션인지 확인)
            if (!skuIds.contains(newSkuId)) {
                throw new IllegalArgumentException(
                        "동일 상품의 다른 옵션으로만 교환 가능합니다. 다른 상품으로의 교환은 지원하지 않습니다.");
            }

            // 새 옵션의 재고 확인
            productDetail.getSkus().stream()
                    .filter(sku -> sku.getId().equals(newSkuId))
                    .findFirst()
                    .ifPresent(sku -> {
                        if (sku.getStockQty() == null || sku.getStockQty() < 1) {
                            throw new IllegalStateException("교환하려는 옵션의 재고가 부족합니다. skuId=" + newSkuId);
                        }
                    });

            log.debug("동일 상품 검증 완료 - productId={}, originalSkuId={}, newSkuId={}",
                    productId, originalSkuId, newSkuId);

        } catch (feign.FeignException.NotFound e) {
            log.warn("상품 정보 조회 실패 - productId={}", productId);
            throw new IllegalArgumentException("상품 정보를 찾을 수 없습니다. productId=" + productId);
        } catch (feign.FeignException e) {
            log.error("상품 서비스 호출 실패 - productId={}", productId, e);
            throw new RuntimeException("상품 정보 조회 중 오류가 발생했습니다.");
        }
    }
}

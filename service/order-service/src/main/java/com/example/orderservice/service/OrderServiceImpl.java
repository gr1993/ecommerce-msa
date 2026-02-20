package com.example.orderservice.service;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.client.PromotionServiceClient;
import com.example.orderservice.client.dto.ApplicableDiscountPolicyResponse;
import com.example.orderservice.client.dto.ProductDetailResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.OptionValueResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.SkuResponse;
import com.example.orderservice.client.dto.UserCouponResponse;
import com.example.orderservice.domain.entity.DiscountType;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderDiscount;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.domain.entity.Outbox;
import com.example.orderservice.domain.event.CouponUsedEvent;
import com.example.orderservice.domain.event.OrderCreatedEvent;
import com.example.orderservice.dto.request.DeliveryInfoRequest;
import com.example.orderservice.dto.request.DiscountRequest;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.request.ShippingSyncRequest;
import com.example.orderservice.dto.response.MyOrderResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.ShippingSyncOrderResponse;
import com.example.orderservice.global.common.EventTypeConstants;
import com.example.orderservice.global.common.dto.PageResponse;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ProductServiceClient productServiceClient;
    private final PromotionServiceClient promotionServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        BigDecimal totalProductAmount = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .userId(userId)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(BigDecimal.ZERO)
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.ZERO)
                .build();

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            ProductDetailResponse product = productServiceClient.getProductDetail(itemRequest.getProductId());

            SkuResponse sku = findSkuById(product.getSkus(), itemRequest.getSkuId());
            String productNameWithOptions = buildProductNameWithOptions(product, sku);
            BigDecimal unitPrice = BigDecimal.valueOf(sku.getPrice());
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .skuId(itemRequest.getSkuId())
                    .productName(productNameWithOptions)
                    .productCode(product.getProductCode())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build();
            order.addOrderItem(orderItem);

            totalProductAmount = totalProductAmount.add(totalPrice);
        }

        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        if (request.getDiscounts() != null && !request.getDiscounts().isEmpty()) {
            List<Long> productIds = request.getOrderItems().stream()
                    .map(OrderItemRequest::getProductId)
                    .distinct()
                    .toList();
            validateDiscounts(userId, request.getDiscounts(), productIds);

            for (DiscountRequest discountRequest : request.getDiscounts()) {
                OrderDiscount orderDiscount = OrderDiscount.builder()
                        .discountType(DiscountType.valueOf(discountRequest.getDiscountType()))
                        .referenceId(discountRequest.getReferenceId())
                        .discountName(discountRequest.getDiscountName())
                        .discountAmount(discountRequest.getDiscountAmount())
                        .discountRate(discountRequest.getDiscountRate())
                        .description(discountRequest.getDescription())
                        .build();
                order.addOrderDiscount(orderDiscount);
                totalDiscountAmount = totalDiscountAmount.add(BigDecimal.valueOf(discountRequest.getDiscountAmount()));
            }
        }

        order.updateTotalAmounts(totalProductAmount, totalDiscountAmount);

        DeliveryInfoRequest deliveryInfo = request.getDeliveryInfo();
        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .receiverPhone(deliveryInfo.getReceiverPhone())
                .zipcode(deliveryInfo.getZipcode())
                .address(deliveryInfo.getAddress())
                .addressDetail(deliveryInfo.getAddressDetail())
                .deliveryMemo(deliveryInfo.getDeliveryMemo())
                .build();
        order.setOrderDelivery(orderDelivery);

        Order savedOrder = orderRepository.save(order);

        saveOrderCreatedOutbox(savedOrder);
        saveCouponUsedOutboxes(savedOrder);

        return OrderResponse.from(savedOrder);
    }

    @Override
    public PageResponse<MyOrderResponse> getMyOrders(Long userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserIdWithItems(userId, pageable);
        Page<MyOrderResponse> responsePage = orderPage.map(MyOrderResponse::from);
        return PageResponse.from(responsePage);
    }

    @Override
    public PageResponse<ShippingSyncOrderResponse> getOrdersForShippingSync(ShippingSyncRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Order> orderPage = orderRepository.findByOrderStatusWithDelivery(OrderStatus.PAID, pageable);
        Page<ShippingSyncOrderResponse> responsePage = orderPage.map(ShippingSyncOrderResponse::from);
        return PageResponse.from(responsePage);
    }

    private void saveOrderCreatedOutbox(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .orderStatus(order.getOrderStatus().name())
                .totalProductAmount(order.getTotalProductAmount())
                .totalDiscountAmount(order.getTotalDiscountAmount())
                .totalPaymentAmount(order.getTotalPaymentAmount())
                .orderItems(order.getOrderItems().stream()
                        .map(item -> OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(item.getId())
                                .productId(item.getProductId())
                                .skuId(item.getSkuId())
                                .productName(item.getProductName())
                                .productCode(item.getProductCode())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList())
                .delivery(OrderCreatedEvent.DeliverySnapshot.builder()
                        .receiverName(order.getOrderDelivery().getReceiverName())
                        .receiverPhone(order.getOrderDelivery().getReceiverPhone())
                        .zipcode(order.getOrderDelivery().getZipcode())
                        .address(order.getOrderDelivery().getAddress())
                        .addressDetail(order.getOrderDelivery().getAddressDetail())
                        .deliveryMemo(order.getOrderDelivery().getDeliveryMemo())
                        .build())
                .orderedAt(order.getOrderedAt())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            Outbox outbox = Outbox.builder()
                    .aggregateType("Order")
                    .aggregateId(String.valueOf(order.getId()))
                    .eventType(EventTypeConstants.TOPIC_ORDER_CREATED)
                    .payload(payload)
                    .build();
            outboxRepository.save(outbox);
            log.debug("Outbox 저장 완료: orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            log.error("OrderCreatedEvent 직렬화 실패: orderId={}", order.getId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    private void validateDiscounts(Long userId, List<DiscountRequest> discounts, List<Long> productIds) {
        List<DiscountRequest> couponDiscounts = discounts.stream()
                .filter(d -> "COUPON".equals(d.getDiscountType()))
                .toList();
        List<DiscountRequest> policyDiscounts = discounts.stream()
                .filter(d -> "POLICY".equals(d.getDiscountType()))
                .toList();

        if (!couponDiscounts.isEmpty()) {
            List<UserCouponResponse> userCoupons = promotionServiceClient.getUserCoupons(userId);
            Map<Long, UserCouponResponse> couponMap = userCoupons.stream()
                    .collect(Collectors.toMap(UserCouponResponse::getUserCouponId, c -> c));

            for (DiscountRequest couponDiscount : couponDiscounts) {
                UserCouponResponse userCoupon = couponMap.get(couponDiscount.getReferenceId());
                if (userCoupon == null) {
                    throw new IllegalArgumentException(
                            "사용자가 보유하지 않은 쿠폰입니다: userCouponId=" + couponDiscount.getReferenceId());
                }
                if (!"ISSUED".equals(userCoupon.getCouponStatus()) && !"RESTORED".equals(userCoupon.getCouponStatus())) {
                    throw new IllegalArgumentException(
                            "사용할 수 없는 상태의 쿠폰입니다: userCouponId=" + couponDiscount.getReferenceId()
                                    + ", status=" + userCoupon.getCouponStatus());
                }
            }
        }

        if (!policyDiscounts.isEmpty()) {
            List<ApplicableDiscountPolicyResponse> applicablePolicies =
                    promotionServiceClient.getApplicableDiscountPolicies(productIds);
            Map<Long, ApplicableDiscountPolicyResponse> policyMap = applicablePolicies.stream()
                    .collect(Collectors.toMap(ApplicableDiscountPolicyResponse::getDiscountId, p -> p));

            for (DiscountRequest policyDiscount : policyDiscounts) {
                if (!policyMap.containsKey(policyDiscount.getReferenceId())) {
                    throw new IllegalArgumentException(
                            "적용할 수 없는 할인 정책입니다: discountPolicyId=" + policyDiscount.getReferenceId());
                }
            }
        }
    }

    private void saveCouponUsedOutboxes(Order order) {
        order.getOrderDiscounts().stream()
                .filter(d -> d.getDiscountType() == DiscountType.COUPON)
                .forEach(discount -> {
                    CouponUsedEvent event = CouponUsedEvent.builder()
                            .orderId(order.getOrderNumber())
                            .userCouponId(discount.getReferenceId())
                            .customerId(String.valueOf(order.getUserId()))
                            .usedAt(order.getOrderedAt())
                            .build();
                    try {
                        String payload = objectMapper.writeValueAsString(event);
                        Outbox outbox = Outbox.builder()
                                .aggregateType("Coupon")
                                .aggregateId(String.valueOf(discount.getReferenceId()))
                                .eventType(EventTypeConstants.TOPIC_COUPON_USED)
                                .payload(payload)
                                .build();
                        outboxRepository.save(outbox);
                        log.debug("CouponUsedEvent Outbox 저장 완료: orderId={}, userCouponId={}",
                                order.getId(), discount.getReferenceId());
                    } catch (JsonProcessingException e) {
                        log.error("CouponUsedEvent 직렬화 실패: orderId={}, userCouponId={}",
                                order.getId(), discount.getReferenceId(), e);
                        throw new RuntimeException("이벤트 직렬화 실패", e);
                    }
                });
    }

    private SkuResponse findSkuById(List<SkuResponse> skus, Long skuId) {
        return skus.stream()
                .filter(sku -> sku.getId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("SKU를 찾을 수 없습니다: " + skuId));
    }

    private String buildProductNameWithOptions(ProductDetailResponse product, SkuResponse sku) {
        if (sku.getOptionValueIds() == null || sku.getOptionValueIds().isEmpty()) {
            return product.getProductName();
        }

        Map<Long, String> optionValueNameMap = product.getOptionGroups().stream()
                .flatMap(group -> group.getOptionValues().stream())
                .collect(Collectors.toMap(OptionValueResponse::getId, OptionValueResponse::getOptionValueName));

        List<String> optionNames = sku.getOptionValueIds().stream()
                .map(optionValueNameMap::get)
                .filter(name -> name != null)
                .toList();

        if (optionNames.isEmpty()) {
            return product.getProductName();
        }

        return product.getProductName() + "(" + String.join(", ", optionNames) + ")";
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + uniquePart;
    }
}

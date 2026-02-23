package com.example.orderservice.service;

import com.example.orderservice.client.PaymentServiceClient;
import com.example.orderservice.client.ShippingServiceClient;
import com.example.orderservice.client.dto.TestCreatePaymentOrderRequest;
import com.example.orderservice.client.dto.TestCreateShippingRequest;
import com.example.orderservice.domain.entity.*;
import com.example.orderservice.dto.request.TestCreateOrderRequest;
import com.example.orderservice.dto.response.TestCreateOrderResponse;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalOrderServiceImpl implements InternalOrderService {

    private final OrderRepository orderRepository;
    private final ShippingServiceClient shippingServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    @Transactional
    public TestCreateOrderResponse createOrderForTest(TestCreateOrderRequest request) {
        log.info("[TEST] 테스트 주문 생성 시작: userId={}", request.getUserId());

        // 주문번호 생성
        String orderNumber = generateOrderNumber();

        // 이미 존재하는 주문번호인지 확인 (거의 발생하지 않겠지만 안전을 위해)
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            orderNumber = generateOrderNumber();
        }

        // 금액 계산
        BigDecimal totalProductAmount = request.calculateTotalProductAmount();
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        BigDecimal totalPaymentAmount = totalProductAmount.subtract(totalDiscountAmount);

        // 주문 생성 (PAID 상태)
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.getUserId())
                .orderStatus(OrderStatus.PAID)
                .totalProductAmount(totalProductAmount)
                .totalDiscountAmount(totalDiscountAmount)
                .totalPaymentAmount(totalPaymentAmount)
                .orderMemo(request.getOrderMemo())
                .build();

        // 주문 상품 추가
        for (TestCreateOrderRequest.OrderItemInfo itemInfo : request.getOrderItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemInfo.getProductId())
                    .skuId(itemInfo.getSkuId())
                    .productName(itemInfo.getProductName())
                    .productCode(itemInfo.getProductCode())
                    .quantity(itemInfo.getQuantity())
                    .unitPrice(itemInfo.getUnitPrice())
                    .totalPrice(itemInfo.getTotalPrice())
                    .build();
            order.addOrderItem(orderItem);
        }

        // 배송 정보 추가
        TestCreateOrderRequest.DeliveryInfo deliveryInfo = request.getDelivery();
        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName(deliveryInfo.getReceiverName())
                .receiverPhone(deliveryInfo.getReceiverPhone())
                .zipcode(deliveryInfo.getZipcode())
                .address(deliveryInfo.getAddress())
                .addressDetail(deliveryInfo.getAddressDetail())
                .deliveryMemo(deliveryInfo.getDeliveryMemo())
                .build();
        order.setOrderDelivery(orderDelivery);

        // 결제 정보 추가 (PAID 상태)
        String paymentKey = "tgen_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        OrderPayment orderPayment = OrderPayment.builder()
                .paymentMethod(PaymentMethod.CARD)
                .paymentAmount(totalPaymentAmount)
                .paymentStatus(PaymentStatus.PAID)
                .paymentKey(paymentKey)
                .paidAt(LocalDateTime.now())
                .build();
        order.addOrderPayment(orderPayment);

        // 주문 저장
        Order savedOrder = orderRepository.save(order);
        log.info("[TEST] 주문 저장 완료: orderId={}, orderNumber={}", savedOrder.getId(), savedOrder.getOrderNumber());

        // shipping-service 테스트 데이터 생성
        boolean shippingCreated = createShippingData(savedOrder, deliveryInfo);

        // payment-service 테스트 데이터 생성
        boolean paymentCreated = createPaymentData(savedOrder, request.getOrderItems(), paymentKey);

        log.info("[TEST] 테스트 주문 생성 완료: orderId={}, shippingCreated={}, paymentCreated={}",
                savedOrder.getId(), shippingCreated, paymentCreated);

        return TestCreateOrderResponse.from(savedOrder, shippingCreated, paymentCreated);
    }

    @Override
    @Transactional
    public void deleteOrderForTest(Long orderId) {
        log.info("[TEST] 테스트 데이터 삭제 시작: orderId={}", orderId);

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.info("[TEST] 삭제할 주문 없음: orderId={}", orderId);
            return;
        }

        Order order = orderOpt.get();
        String orderNumber = order.getOrderNumber();

        // shipping-service 테스트 데이터 삭제
        deleteShippingData(orderId);

        // payment-service 테스트 데이터 삭제
        deletePaymentData(orderNumber);

        // 주문 삭제
        orderRepository.delete(order);
        log.info("[TEST] 주문 삭제 완료: orderId={}, orderNumber={}", orderId, orderNumber);
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + datePrefix + "-" + uniqueSuffix;
    }

    private boolean createShippingData(Order order, TestCreateOrderRequest.DeliveryInfo deliveryInfo) {
        try {
            TestCreateShippingRequest shippingRequest = TestCreateShippingRequest.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .delivery(TestCreateShippingRequest.DeliveryInfo.builder()
                            .receiverName(deliveryInfo.getReceiverName())
                            .receiverPhone(deliveryInfo.getReceiverPhone())
                            .zipcode(deliveryInfo.getZipcode())
                            .address(deliveryInfo.getAddress())
                            .addressDetail(deliveryInfo.getAddressDetail())
                            .build())
                    .build();

            shippingServiceClient.createShippingForTest(shippingRequest);
            log.info("[TEST] shipping-service 데이터 생성 성공: orderId={}", order.getId());
            return true;
        } catch (Exception e) {
            log.warn("[TEST] shipping-service 데이터 생성 실패: orderId={}, error={}", order.getId(), e.getMessage());
            return false;
        }
    }

    private boolean createPaymentData(Order order, java.util.List<TestCreateOrderRequest.OrderItemInfo> orderItems, String paymentKey) {
        try {
            TestCreatePaymentOrderRequest paymentRequest = TestCreatePaymentOrderRequest.builder()
                    .orderNumber(order.getOrderNumber())
                    .userId(order.getUserId())
                    .totalPaymentAmount(order.getTotalPaymentAmount())
                    .orderItems(orderItems.stream()
                            .map(item -> TestCreatePaymentOrderRequest.OrderItemInfo.builder()
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .paymentKey(paymentKey)
                    .build();

            paymentServiceClient.createOrderForTest(paymentRequest);
            log.info("[TEST] payment-service 데이터 생성 성공: orderNumber={}", order.getOrderNumber());
            return true;
        } catch (Exception e) {
            log.warn("[TEST] payment-service 데이터 생성 실패: orderNumber={}, error={}", order.getOrderNumber(), e.getMessage());
            return false;
        }
    }

    private void deleteShippingData(Long orderId) {
        try {
            shippingServiceClient.deleteShippingForTest(orderId);
            log.info("[TEST] shipping-service 데이터 삭제 성공: orderId={}", orderId);
        } catch (Exception e) {
            log.warn("[TEST] shipping-service 데이터 삭제 실패: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    private void deletePaymentData(String orderNumber) {
        try {
            paymentServiceClient.deleteOrderForTest(orderNumber);
            log.info("[TEST] payment-service 데이터 삭제 성공: orderNumber={}", orderNumber);
        } catch (Exception e) {
            log.warn("[TEST] payment-service 데이터 삭제 실패: orderNumber={}, error={}", orderNumber, e.getMessage());
        }
    }
}

package com.example.productservice.consumer;

import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.product.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer 테스트")
class OrderEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    private OrderCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        OrderCreatedEvent.OrderItemSnapshot orderItem1 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .productId(1L)
                .skuId(1L)
                .productName("테스트 상품 1")
                .productCode("TEST-001")
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("20000"))
                .build();

        OrderCreatedEvent.OrderItemSnapshot orderItem2 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(2L)
                .productId(2L)
                .skuId(2L)
                .productName("테스트 상품 2")
                .productCode("TEST-002")
                .quantity(1)
                .unitPrice(new BigDecimal("30000"))
                .totalPrice(new BigDecimal("30000"))
                .build();

        OrderCreatedEvent.DeliverySnapshot delivery = OrderCreatedEvent.DeliverySnapshot.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울시 강남구")
                .addressDetail("테헤란로 123")
                .deliveryMemo("문 앞에 놔주세요")
                .build();

        testEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-0001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("50000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000"))
                .orderItems(List.of(orderItem1, orderItem2))
                .delivery(delivery)
                .orderedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 정상 처리")
    void consumeOrderCreatedEvent_success() {
        // given
        doNothing().when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when
        orderEventConsumer.consumeOrderCreatedEvent(
                testEvent,
                "order.created",
                0L
        );

        // then
        verify(inventoryService, times(1)).decreaseStock(testEvent);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 단일 상품")
    void consumeOrderCreatedEvent_singleItem() {
        // given
        OrderCreatedEvent.OrderItemSnapshot singleItem = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .productId(1L)
                .skuId(1L)
                .productName("단일 상품")
                .productCode("SINGLE-001")
                .quantity(1)
                .unitPrice(new BigDecimal("50000"))
                .totalPrice(new BigDecimal("50000"))
                .build();

        OrderCreatedEvent singleItemEvent = OrderCreatedEvent.builder()
                .orderId(2L)
                .orderNumber("ORD-20240101-0002")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("50000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000"))
                .orderItems(List.of(singleItem))
                .orderedAt(LocalDateTime.now())
                .build();

        doNothing().when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when
        orderEventConsumer.consumeOrderCreatedEvent(
                singleItemEvent,
                "order.created",
                1L
        );

        // then
        verify(inventoryService, times(1)).decreaseStock(singleItemEvent);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 재고 차감 실패 시 예외 전파")
    void consumeOrderCreatedEvent_inventoryServiceThrowsException() {
        // given
        doThrow(new IllegalArgumentException("SKU not found: skuId=1"))
                .when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when & then
        assertThatThrownBy(() -> orderEventConsumer.consumeOrderCreatedEvent(
                testEvent,
                "order.created",
                0L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU not found");

        verify(inventoryService, times(1)).decreaseStock(testEvent);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 재고 부족 예외 전파")
    void consumeOrderCreatedEvent_insufficientStock() {
        // given
        doThrow(new IllegalStateException("Insufficient stock: skuId=1, currentStock=0, requestedQty=2"))
                .when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when & then
        assertThatThrownBy(() -> orderEventConsumer.consumeOrderCreatedEvent(
                testEvent,
                "order.created",
                0L
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");

        verify(inventoryService, times(1)).decreaseStock(testEvent);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 일반 예외 처리")
    void consumeOrderCreatedEvent_generalException() {
        // given
        doThrow(new RuntimeException("Database connection failed"))
                .when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when & then
        assertThatThrownBy(() -> orderEventConsumer.consumeOrderCreatedEvent(
                testEvent,
                "order.created",
                0L
        ))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        verify(inventoryService, times(1)).decreaseStock(testEvent);
    }

    @Test
    @DisplayName("DLT 핸들러 - OrderCreatedEvent 타입")
    void handleDlt_withOrderCreatedEvent() {
        // given
        String topic = "order.created-dlt";
        String originalTopic = "order.created";
        Long offset = 5L;
        String exceptionMessage = "SKU not found: skuId=1";

        // when
        orderEventConsumer.handleDlt(
                testEvent,
                topic,
                offset,
                originalTopic,
                exceptionMessage
        );

        // then
        // DLT 핸들러는 로깅만 수행하므로 예외가 발생하지 않아야 함
        verify(inventoryService, never()).decreaseStock(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("DLT 핸들러 - 알 수 없는 payload 타입")
    void handleDlt_withUnknownPayload() {
        // given
        Object unknownPayload = "Unknown payload";
        String topic = "order.created-dlt";
        String originalTopic = "order.created";
        Long offset = 5L;
        String exceptionMessage = "Unknown error";

        // when
        orderEventConsumer.handleDlt(
                unknownPayload,
                topic,
                offset,
                originalTopic,
                exceptionMessage
        );

        // then
        // DLT 핸들러는 로깅만 수행하므로 예외가 발생하지 않아야 함
        verify(inventoryService, never()).decreaseStock(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 여러 상품 주문")
    void consumeOrderCreatedEvent_multipleItems() {
        // given
        OrderCreatedEvent.OrderItemSnapshot item1 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .skuId(1L)
                .productName("상품1")
                .quantity(2)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("20000"))
                .build();

        OrderCreatedEvent.OrderItemSnapshot item2 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(2L)
                .skuId(2L)
                .productName("상품2")
                .quantity(3)
                .unitPrice(new BigDecimal("15000"))
                .totalPrice(new BigDecimal("45000"))
                .build();

        OrderCreatedEvent.OrderItemSnapshot item3 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(3L)
                .skuId(3L)
                .productName("상품3")
                .quantity(1)
                .unitPrice(new BigDecimal("50000"))
                .totalPrice(new BigDecimal("50000"))
                .build();

        OrderCreatedEvent multiItemEvent = OrderCreatedEvent.builder()
                .orderId(3L)
                .orderNumber("ORD-20240101-0003")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("115000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("115000"))
                .orderItems(List.of(item1, item2, item3))
                .orderedAt(LocalDateTime.now())
                .build();

        doNothing().when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when
        orderEventConsumer.consumeOrderCreatedEvent(
                multiItemEvent,
                "order.created",
                2L
        );

        // then
        verify(inventoryService, times(1)).decreaseStock(multiItemEvent);
    }

    @Test
    @DisplayName("주문 생성 이벤트 수신 - 할인이 적용된 주문")
    void consumeOrderCreatedEvent_withDiscount() {
        // given
        OrderCreatedEvent.OrderItemSnapshot item = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .skuId(1L)
                .productName("할인 상품")
                .quantity(1)
                .unitPrice(new BigDecimal("100000"))
                .totalPrice(new BigDecimal("100000"))
                .build();

        OrderCreatedEvent discountEvent = OrderCreatedEvent.builder()
                .orderId(4L)
                .orderNumber("ORD-20240101-0004")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("100000"))
                .totalDiscountAmount(new BigDecimal("20000"))
                .totalPaymentAmount(new BigDecimal("80000"))
                .orderItems(List.of(item))
                .orderedAt(LocalDateTime.now())
                .build();

        doNothing().when(inventoryService).decreaseStock(any(OrderCreatedEvent.class));

        // when
        orderEventConsumer.consumeOrderCreatedEvent(
                discountEvent,
                "order.created",
                3L
        );

        // then
        verify(inventoryService, times(1)).decreaseStock(discountEvent);
    }
}

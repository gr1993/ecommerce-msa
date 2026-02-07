package com.example.productservice.product.service;

import com.example.productservice.consumer.domain.ProcessedEvent;
import com.example.productservice.consumer.event.OrderCancelledEvent;
import com.example.productservice.consumer.event.PaymentCancelledEvent;
import com.example.productservice.consumer.repository.ProcessedEventRepository;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockRestoreCompensationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private ProductSku testSku;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 및 SKU 생성
        testProduct = Product.builder()
                .productName("Test Product")
                .productCode("TEST-001")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(9000))
                .status("AVAILABLE")
                .isDisplayed(true)
                .build();
        productRepository.save(testProduct);

        testSku = ProductSku.builder()
                .product(testProduct)
                .skuCode("SKU-001")
                .price(BigDecimal.valueOf(10000))
                .stockQty(50)  // 재고 차감 후 상태 (원래 100 - 50 = 50)
                .status("AVAILABLE")
                .build();
        productSkuRepository.save(testSku);
    }

    // ===== order.cancelled 테스트 =====

    @Test
    @DisplayName("주문 취소 이벤트 처리 시 재고가 복구되어야 한다 (order.cancelled)")
    void restoreStockForOrderCancelled_shouldRecoverStock() {
        // given
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(99999L)
                .orderNumber("ORD-CANCEL-001")
                .cancellationReason("USER_REQUEST")
                .userId(1L)
                .cancelledItems(List.of(
                        OrderCancelledEvent.CancelledOrderItem.builder()
                                .orderItemId(1L)
                                .productId(testProduct.getProductId())
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .productCode("TEST-001")
                                .quantity(10)
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();
        int expectedStock = initialStock + 10;

        // when
        inventoryService.restoreStockForOrderCancelled(event);
        productSkuRepository.flush();

        // then
        ProductSku afterRecovery = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterRecovery.getStockQty())
                .isEqualTo(expectedStock)
                .describedAs("재고 복구 (order.cancelled): 차감된 재고가 다시 증가해야 함");

        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "ORDER_CANCELLED", event.getOrderId().toString())).isTrue();
    }

    @Test
    @DisplayName("같은 주문 취소 이벤트를 두 번 처리하면 재고는 한 번만 복구되어야 한다 (멱등성)")
    void restoreStockForOrderCancelled_idempotency() {
        // given
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(88888L)
                .orderNumber("ORD-CANCEL-002")
                .cancellationReason("USER_REQUEST")
                .userId(1L)
                .cancelledItems(List.of(
                        OrderCancelledEvent.CancelledOrderItem.builder()
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .quantity(10)
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();
        int expectedStock = initialStock + 10;

        // when - 첫 번째 처리
        inventoryService.restoreStockForOrderCancelled(event);
        productSkuRepository.flush();

        // then - 재고 복구
        ProductSku afterFirst = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterFirst.getStockQty()).isEqualTo(expectedStock);

        // when - 두 번째 처리 (멱등성)
        inventoryService.restoreStockForOrderCancelled(event);
        productSkuRepository.flush();

        // then - 재고 추가 증가 없음
        ProductSku afterSecond = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterSecond.getStockQty())
                .isEqualTo(expectedStock)
                .describedAs("멱등성 보장: 동일한 주문 취소는 한 번만 처리");

        // 처리 이력 1개만 존재
        long count = processedEventRepository.findAll().stream()
                .filter(e -> "ORDER_CANCELLED".equals(e.getEventType()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    // ===== payment.cancelled 테스트 =====

    @Test
    @DisplayName("결제 취소 이벤트 처리 시 재고가 복구되어야 한다 (payment.cancelled)")
    void restoreStockForPaymentCancelled_shouldRecoverStock() {
        // given
        PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                .orderId(77777L)
                .orderNumber("ORD-PAY-FAIL-001")
                .paymentId(12345L)
                .cancellationReason("PAYMENT_TIMEOUT")
                .userId(1L)
                .items(List.of(
                        PaymentCancelledEvent.PaymentItem.builder()
                                .orderItemId(1L)
                                .productId(testProduct.getProductId())
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .productCode("TEST-001")
                                .quantity(15)
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(150000))
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();
        int expectedStock = initialStock + 15;

        // when
        inventoryService.restoreStockForPaymentCancelled(event);
        productSkuRepository.flush();

        // then
        ProductSku afterRecovery = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterRecovery.getStockQty())
                .isEqualTo(expectedStock)
                .describedAs("재고 복구 (payment.cancelled): 차감된 재고가 다시 증가해야 함");

        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "PAYMENT_CANCELLED", event.getOrderId().toString())).isTrue();
    }

    @Test
    @DisplayName("같은 결제 취소 이벤트를 두 번 처리하면 재고는 한 번만 복구되어야 한다 (멱등성)")
    void restoreStockForPaymentCancelled_idempotency() {
        // given
        PaymentCancelledEvent event = PaymentCancelledEvent.builder()
                .orderId(66666L)
                .orderNumber("ORD-PAY-FAIL-002")
                .paymentId(54321L)
                .cancellationReason("PAYMENT_FAILED")
                .userId(1L)
                .items(List.of(
                        PaymentCancelledEvent.PaymentItem.builder()
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .quantity(20)
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();
        int expectedStock = initialStock + 20;

        // when - 첫 번째 처리
        inventoryService.restoreStockForPaymentCancelled(event);
        productSkuRepository.flush();

        // then - 재고 복구
        ProductSku afterFirst = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterFirst.getStockQty()).isEqualTo(expectedStock);

        // when - 두 번째 처리 (멱등성)
        inventoryService.restoreStockForPaymentCancelled(event);
        productSkuRepository.flush();

        // then - 재고 추가 증가 없음
        ProductSku afterSecond = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterSecond.getStockQty())
                .isEqualTo(expectedStock)
                .describedAs("멱등성 보장: 동일한 결제 취소는 한 번만 처리");

        // 처리 이력 1개만 존재
        long count = processedEventRepository.findAll().stream()
                .filter(e -> "PAYMENT_CANCELLED".equals(e.getEventType()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 취소와 결제 취소는 서로 다른 이벤트로 구분되어야 한다")
    void orderCancelledAndPaymentCancelled_shouldBeSeparateEvents() {
        // given
        Long orderId = 55555L;

        OrderCancelledEvent orderEvent = OrderCancelledEvent.builder()
                .orderId(orderId)
                .orderNumber("ORD-BOTH-001")
                .cancellationReason("USER_REQUEST")
                .cancelledItems(List.of(
                        OrderCancelledEvent.CancelledOrderItem.builder()
                                .skuId(testSku.getSkuId())
                                .quantity(5)
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        PaymentCancelledEvent paymentEvent = PaymentCancelledEvent.builder()
                .orderId(orderId)
                .orderNumber("ORD-BOTH-001")
                .paymentId(999L)
                .cancellationReason("PAYMENT_FAILED")
                .items(List.of(
                        PaymentCancelledEvent.PaymentItem.builder()
                                .skuId(testSku.getSkuId())
                                .quantity(3)
                                .build()
                ))
                .cancelledAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();

        // when - 두 이벤트 모두 처리 (같은 orderId지만 다른 이벤트 타입)
        inventoryService.restoreStockForOrderCancelled(orderEvent);
        inventoryService.restoreStockForPaymentCancelled(paymentEvent);
        productSkuRepository.flush();

        // then - 둘 다 처리되어 재고가 8 증가
        ProductSku sku = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(sku.getStockQty()).isEqualTo(initialStock + 5 + 3);

        // 두 개의 처리 이력 존재
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "ORDER_CANCELLED", orderId.toString())).isTrue();
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "PAYMENT_CANCELLED", orderId.toString())).isTrue();
    }
}

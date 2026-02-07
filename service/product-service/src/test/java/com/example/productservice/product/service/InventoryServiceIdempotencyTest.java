package com.example.productservice.product.service;

import com.example.productservice.consumer.domain.ProcessedEvent;
import com.example.productservice.consumer.event.OrderCreatedEvent;
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
class InventoryServiceIdempotencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private ProductSku testSku;
    private OrderCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 및 SKU 생성
        Product product = Product.builder()
                .productName("Test Product")
                .productCode("TEST-001")
                .basePrice(BigDecimal.valueOf(10000))
                .salePrice(BigDecimal.valueOf(9000))
                .status("AVAILABLE")
                .isDisplayed(true)
                .build();
        productRepository.save(product);

        testSku = ProductSku.builder()
                .product(product)
                .skuCode("SKU-001")
                .price(BigDecimal.valueOf(10000))
                .stockQty(100)
                .status("AVAILABLE")
                .build();
        productSkuRepository.save(testSku);

        // 테스트용 주문 이벤트 생성
        testEvent = OrderCreatedEvent.builder()
                .orderId(12345L)
                .orderNumber("ORD-2024-001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(20000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(20000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(1L)
                                .productId(product.getProductId())
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .productCode("TEST-001")
                                .quantity(10)
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("같은 주문 이벤트를 두 번 처리하면 재고는 한 번만 차감되어야 한다")
    void decreaseStock_idempotency_shouldProcessOnlyOnce() {
        // given
        int initialStock = testSku.getStockQty();
        int expectedStock = initialStock - 10;

        // when - 첫 번째 처리
        inventoryService.decreaseStock(testEvent);
        productSkuRepository.flush();
        ProductSku afterFirst = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();

        // then - 재고가 정상적으로 차감됨
        assertThat(afterFirst.getStockQty()).isEqualTo(expectedStock);
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "ORDER_CREATED", testEvent.getOrderId().toString())).isTrue();

        // when - 동일한 이벤트 두 번째 처리 (멱등성 체크)
        inventoryService.decreaseStock(testEvent);
        productSkuRepository.flush();
        ProductSku afterSecond = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();

        // then - 재고가 추가로 차감되지 않음
        assertThat(afterSecond.getStockQty())
                .isEqualTo(expectedStock)
                .describedAs("멱등성 보장: 동일한 주문은 한 번만 처리되어야 함");

        // 처리 이력이 하나만 존재해야 함
        List<ProcessedEvent> processedEvents = processedEventRepository.findAll();
        assertThat(processedEvents)
                .hasSize(1)
                .first()
                .satisfies(event -> {
                    assertThat(event.getAggregateId()).isEqualTo(testEvent.getOrderId().toString());
                    assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
                });
    }

    @Test
    @DisplayName("이미 처리된 주문 이벤트는 멱등성 체크로 스킵되어야 한다")
    void decreaseStock_alreadyProcessed_shouldSkip() {
        // given - 이미 처리된 것으로 기록
        ProcessedEvent alreadyProcessed = ProcessedEvent.ofOrderCreated(
                testEvent.getOrderId(),
                testEvent.getOrderNumber()
        );
        processedEventRepository.save(alreadyProcessed);

        int initialStock = testSku.getStockQty();

        // when
        inventoryService.decreaseStock(testEvent);
        productSkuRepository.flush();

        // then - 재고가 차감되지 않음
        ProductSku sku = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(sku.getStockQty())
                .isEqualTo(initialStock)
                .describedAs("이미 처리된 주문은 재고 차감이 발생하지 않아야 함");
    }

    @Test
    @DisplayName("서로 다른 주문 이벤트는 각각 처리되어야 한다")
    void decreaseStock_differentOrders_shouldProcessSeparately() {
        // given
        int initialStock = testSku.getStockQty();

        // 첫 번째 주문 이벤트
        OrderCreatedEvent firstEvent = OrderCreatedEvent.builder()
                .orderId(100L)
                .orderNumber("ORD-100")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(10000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(10000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(1L)
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .quantity(5)
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(50000))
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();

        // 두 번째 주문 이벤트
        OrderCreatedEvent secondEvent = OrderCreatedEvent.builder()
                .orderId(200L)
                .orderNumber("ORD-200")
                .userId(2L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(10000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(10000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(2L)
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .quantity(3)
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(30000))
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();

        // when
        inventoryService.decreaseStock(firstEvent);
        inventoryService.decreaseStock(secondEvent);
        productSkuRepository.flush();

        // then
        ProductSku sku = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(sku.getStockQty()).isEqualTo(initialStock - 5 - 3);

        // 두 개의 처리 이력이 존재해야 함
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId("ORDER_CREATED", "100")).isTrue();
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId("ORDER_CREATED", "200")).isTrue();
        assertThat(processedEventRepository.findAll()).hasSize(2);
    }
}

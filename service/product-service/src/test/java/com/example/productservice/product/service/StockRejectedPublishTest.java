package com.example.productservice.product.service;

import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.consumer.repository.ProcessedEventRepository;
import com.example.productservice.global.common.EventTypeConstants;
import com.example.productservice.global.domain.Outbox;
import com.example.productservice.global.repository.OutboxRepository;
import com.example.productservice.product.domain.Product;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.domain.event.StockRejectedEvent;
import com.example.productservice.product.repository.ProductRepository;
import com.example.productservice.product.repository.ProductSkuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class StockRejectedPublishTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

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
                .stockQty(5)  // 재고 부족 시나리오: 5개만 있음
                .status("AVAILABLE")
                .build();
        productSkuRepository.save(testSku);
    }

    @Test
    @DisplayName("재고 부족 시 stock.rejected 이벤트가 Outbox에 저장되어야 한다")
    void decreaseStock_insufficientStock_shouldPublishStockRejectedEvent() throws Exception {
        // given
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(99999L)
                .orderNumber("ORD-INSUFFICIENT-001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(100000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(100000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(1L)
                                .productId(testProduct.getProductId())
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .productCode("TEST-001")
                                .quantity(10)  // 요청 10개, 재고 5개 → 부족
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();

        int initialStock = testSku.getStockQty();

        // when
        inventoryService.decreaseStock(event);
        productSkuRepository.flush();

        // then - 재고가 차감되지 않음
        ProductSku afterProcess = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterProcess.getStockQty())
                .isEqualTo(initialStock)
                .describedAs("재고 부족 시 재고 차감이 발생하지 않아야 함");

        // then - ProcessedEvent에 기록되지 않음 (처리 실패)
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "ORDER_CREATED", event.getOrderId().toString())).isFalse();

        // then - Outbox에 stock.rejected 이벤트 저장됨
        List<Outbox> outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        Outbox outbox = outboxEvents.get(0);
        assertThat(outbox.getEventType()).isEqualTo(EventTypeConstants.TOPIC_STOCK_REJECTED);
        assertThat(outbox.getAggregateType()).isEqualTo("Order");
        assertThat(outbox.getAggregateId()).isEqualTo(event.getOrderId().toString());
        assertThat(outbox.getStatus()).isEqualTo(Outbox.OutboxStatus.PENDING);

        // then - Outbox payload 검증
        StockRejectedEvent stockRejectedEvent = objectMapper.readValue(
                outbox.getPayload(), StockRejectedEvent.class);

        assertThat(stockRejectedEvent.getOrderId()).isEqualTo(event.getOrderId());
        assertThat(stockRejectedEvent.getOrderNumber()).isEqualTo(event.getOrderNumber());
        assertThat(stockRejectedEvent.getRejectionReason()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(stockRejectedEvent.getRejectedItems()).hasSize(1);

        StockRejectedEvent.RejectedItem rejectedItem = stockRejectedEvent.getRejectedItems().get(0);
        assertThat(rejectedItem.getSkuId()).isEqualTo(testSku.getSkuId());
        assertThat(rejectedItem.getRequestedQuantity()).isEqualTo(10);
        assertThat(rejectedItem.getAvailableStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("재고가 충분하면 정상 차감되고 stock.rejected 이벤트가 발행되지 않아야 한다")
    void decreaseStock_sufficientStock_shouldNotPublishStockRejectedEvent() {
        // given
        testSku.setStockQty(100); // 재고 충분
        productSkuRepository.save(testSku);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(88888L)
                .orderNumber("ORD-SUFFICIENT-001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(100000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(100000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(1L)
                                .productId(testProduct.getProductId())
                                .skuId(testSku.getSkuId())
                                .productName("Test Product")
                                .productCode("TEST-001")
                                .quantity(10)  // 요청 10개, 재고 100개 → 충분
                                .unitPrice(BigDecimal.valueOf(10000))
                                .totalPrice(BigDecimal.valueOf(100000))
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();

        // when
        inventoryService.decreaseStock(event);
        productSkuRepository.flush();

        // then - 재고가 정상 차감됨
        ProductSku afterProcess = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterProcess.getStockQty()).isEqualTo(90);

        // then - ProcessedEvent에 기록됨 (처리 성공)
        assertThat(processedEventRepository.existsByEventTypeAndAggregateId(
                "ORDER_CREATED", event.getOrderId().toString())).isTrue();

        // then - Outbox에 stock.rejected 이벤트가 없음
        List<Outbox> outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).isEmpty();
    }

    @Test
    @DisplayName("일부 상품만 재고 부족인 경우 stock.rejected 이벤트가 발행되어야 한다")
    void decreaseStock_partialInsufficientStock_shouldPublishStockRejectedEvent() throws Exception {
        // given - 두 번째 SKU 생성 (재고 충분)
        ProductSku sku2 = ProductSku.builder()
                .product(testProduct)
                .skuCode("SKU-002")
                .price(BigDecimal.valueOf(20000))
                .stockQty(100)  // 재고 충분
                .status("AVAILABLE")
                .build();
        productSkuRepository.save(sku2);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(77777L)
                .orderNumber("ORD-PARTIAL-001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(BigDecimal.valueOf(300000))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(BigDecimal.valueOf(300000))
                .orderItems(List.of(
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(1L)
                                .skuId(testSku.getSkuId())
                                .productName("Test Product 1")
                                .quantity(10)  // 요청 10개, 재고 5개 → 부족
                                .build(),
                        OrderCreatedEvent.OrderItemSnapshot.builder()
                                .orderItemId(2L)
                                .skuId(sku2.getSkuId())
                                .productName("Test Product 2")
                                .quantity(5)   // 요청 5개, 재고 100개 → 충분
                                .build()
                ))
                .orderedAt(LocalDateTime.now())
                .build();

        // when
        inventoryService.decreaseStock(event);
        productSkuRepository.flush();

        // then - 첫 번째 SKU는 재고 차감 안 됨
        ProductSku afterSku1 = productSkuRepository.findById(testSku.getSkuId()).orElseThrow();
        assertThat(afterSku1.getStockQty()).isEqualTo(5);

        // then - 두 번째 SKU는 재고 차감됨 (부분 처리)
        ProductSku afterSku2 = productSkuRepository.findById(sku2.getSkuId()).orElseThrow();
        assertThat(afterSku2.getStockQty()).isEqualTo(95);

        // then - stock.rejected 이벤트 발행됨
        List<Outbox> outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        StockRejectedEvent stockRejectedEvent = objectMapper.readValue(
                outboxEvents.get(0).getPayload(), StockRejectedEvent.class);

        // 재고 부족 항목만 포함
        assertThat(stockRejectedEvent.getRejectedItems()).hasSize(1);
        assertThat(stockRejectedEvent.getRejectedItems().get(0).getSkuId()).isEqualTo(testSku.getSkuId());
    }
}

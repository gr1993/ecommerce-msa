package com.example.productservice.product.service;

import com.example.productservice.consumer.event.OrderCreatedEvent;
import com.example.productservice.product.domain.ProductSku;
import com.example.productservice.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService 테스트")
class InventoryServiceImplTest {

    @Mock
    private ProductSkuRepository productSkuRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private ProductSku testSku;
    private OrderCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        testSku = ProductSku.builder()
                .skuId(1L)
                .skuCode("TEST-SKU-001")
                .stockQty(100)
                .price(new BigDecimal("10000"))
                .status("ACTIVE")
                .build();

        OrderCreatedEvent.OrderItemSnapshot orderItem = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .productId(1L)
                .skuId(1L)
                .productName("테스트 상품")
                .productCode("TEST-001")
                .quantity(10)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("100000"))
                .build();

        testEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-0001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("100000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("100000"))
                .orderItems(List.of(orderItem))
                .build();
    }

    @Test
    @DisplayName("재고 차감 - 정상 처리")
    void decreaseStock_success() {
        // given
        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenReturn(testSku);

        // when
        inventoryService.decreaseStock(testEvent);

        // then
        assertThat(testSku.getStockQty()).isEqualTo(90);
        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, times(1)).save(testSku);
    }

    @Test
    @DisplayName("재고 차감 - 여러 SKU 동시 처리")
    void decreaseStock_multipleSkus() {
        // given
        ProductSku sku1 = ProductSku.builder()
                .skuId(1L)
                .skuCode("SKU-001")
                .stockQty(100)
                .price(new BigDecimal("10000"))
                .status("ACTIVE")
                .build();

        ProductSku sku2 = ProductSku.builder()
                .skuId(2L)
                .skuCode("SKU-002")
                .stockQty(50)
                .price(new BigDecimal("20000"))
                .status("ACTIVE")
                .build();

        OrderCreatedEvent.OrderItemSnapshot item1 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .skuId(1L)
                .productName("상품1")
                .quantity(10)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("100000"))
                .build();

        OrderCreatedEvent.OrderItemSnapshot item2 = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(2L)
                .skuId(2L)
                .productName("상품2")
                .quantity(5)
                .unitPrice(new BigDecimal("20000"))
                .totalPrice(new BigDecimal("100000"))
                .build();

        OrderCreatedEvent multiItemEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-0001")
                .userId(1L)
                .orderStatus("PENDING")
                .totalProductAmount(new BigDecimal("200000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("200000"))
                .orderItems(List.of(item1, item2))
                .build();

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku1));
        when(productSkuRepository.findById(2L)).thenReturn(Optional.of(sku2));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        inventoryService.decreaseStock(multiItemEvent);

        // then
        assertThat(sku1.getStockQty()).isEqualTo(90);
        assertThat(sku2.getStockQty()).isEqualTo(45);
        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, times(1)).findById(2L);
        verify(productSkuRepository, times(2)).save(any(ProductSku.class));
    }

    @Test
    @DisplayName("재고 차감 - SKU를 찾을 수 없음")
    void decreaseStock_skuNotFound() {
        // given
        when(productSkuRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.decreaseStock(testEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU not found: skuId=1");

        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, never()).save(any(ProductSku.class));
    }

    @Test
    @DisplayName("재고 차감 - 재고 부족")
    void decreaseStock_insufficientStock() {
        // given
        testSku.setStockQty(5);  // 요청 수량(10)보다 적은 재고

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));

        // when & then
        assertThatThrownBy(() -> inventoryService.decreaseStock(testEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("skuId=1")
                .hasMessageContaining("currentStock=5")
                .hasMessageContaining("requestedQty=10");

        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, never()).save(any(ProductSku.class));
    }

    @Test
    @DisplayName("재고 차감 - 재고가 정확히 0이 되는 경우")
    void decreaseStock_stockBecomesZero() {
        // given
        testSku.setStockQty(10);  // 요청 수량과 동일한 재고

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenReturn(testSku);

        // when
        inventoryService.decreaseStock(testEvent);

        // then
        assertThat(testSku.getStockQty()).isEqualTo(0);
        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, times(1)).save(testSku);
    }

    @Test
    @DisplayName("재고 차감 - 재고가 0일 때 추가 차감 시도")
    void decreaseStock_alreadyZeroStock() {
        // given
        testSku.setStockQty(0);

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));

        // when & then
        assertThatThrownBy(() -> inventoryService.decreaseStock(testEvent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("currentStock=0");

        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, never()).save(any(ProductSku.class));
    }

    @Test
    @DisplayName("재고 차감 - 수량이 1인 경우")
    void decreaseStock_quantityOne() {
        // given
        OrderCreatedEvent.OrderItemSnapshot singleItem = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .skuId(1L)
                .productName("테스트 상품")
                .quantity(1)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("10000"))
                .build();

        OrderCreatedEvent singleItemEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-0001")
                .userId(1L)
                .orderStatus("PENDING")
                .orderItems(List.of(singleItem))
                .build();

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenReturn(testSku);

        // when
        inventoryService.decreaseStock(singleItemEvent);

        // then
        assertThat(testSku.getStockQty()).isEqualTo(99);
        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, times(1)).save(testSku);
    }

    @Test
    @DisplayName("재고 차감 - 대량 수량 처리")
    void decreaseStock_largeQuantity() {
        // given
        testSku.setStockQty(1000);

        OrderCreatedEvent.OrderItemSnapshot largeItem = OrderCreatedEvent.OrderItemSnapshot.builder()
                .orderItemId(1L)
                .skuId(1L)
                .productName("테스트 상품")
                .quantity(500)
                .unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("5000000"))
                .build();

        OrderCreatedEvent largeQtyEvent = OrderCreatedEvent.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-0001")
                .userId(1L)
                .orderStatus("PENDING")
                .orderItems(List.of(largeItem))
                .build();

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(testSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenReturn(testSku);

        // when
        inventoryService.decreaseStock(largeQtyEvent);

        // then
        assertThat(testSku.getStockQty()).isEqualTo(500);
        verify(productSkuRepository, times(1)).findById(1L);
        verify(productSkuRepository, times(1)).save(testSku);
    }
}

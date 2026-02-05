package com.example.orderservice.service;

import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderDelivery;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.DeliveryInfoRequest;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.client.dto.ProductDetailResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.OptionGroupResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.OptionValueResponse;
import com.example.orderservice.client.dto.ProductDetailResponse.SkuResponse;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderCreateRequest createRequest;
    private DeliveryInfoRequest deliveryInfoRequest;
    private Order savedOrder;
    private ProductDetailResponse productDetailResponse;

    @BeforeEach
    void setUp() {
        deliveryInfoRequest = DeliveryInfoRequest.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(456L)
                .skuId(789L)
                .quantity(2)
                .build();

        createRequest = OrderCreateRequest.builder()
                .orderItems(List.of(itemRequest))
                .deliveryInfo(deliveryInfoRequest)
                .build();

        productDetailResponse = createProductDetailResponse();
        savedOrder = createTestOrder();
    }

    private ProductDetailResponse createProductDetailResponse() {
        OptionValueResponse colorRed = OptionValueResponse.builder()
                .id(1L)
                .optionValueName("빨강")
                .displayOrder(1)
                .build();

        OptionValueResponse sizeL = OptionValueResponse.builder()
                .id(2L)
                .optionValueName("L")
                .displayOrder(1)
                .build();

        OptionGroupResponse colorGroup = OptionGroupResponse.builder()
                .id(1L)
                .optionGroupName("색상")
                .displayOrder(1)
                .optionValues(List.of(colorRed))
                .build();

        OptionGroupResponse sizeGroup = OptionGroupResponse.builder()
                .id(2L)
                .optionGroupName("사이즈")
                .displayOrder(2)
                .optionValues(List.of(sizeL))
                .build();

        SkuResponse sku = SkuResponse.builder()
                .id(789L)
                .skuCode("SKU-001")
                .price(25000L)
                .stockQty(100)
                .status("ON_SALE")
                .optionValueIds(List.of(1L, 2L))
                .build();

        return ProductDetailResponse.builder()
                .productId(456L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .basePrice(30000L)
                .salePrice(25000L)
                .status("ON_SALE")
                .optionGroups(List.of(colorGroup, sizeGroup))
                .skus(List.of(sku))
                .build();
    }

    private Order createTestOrder() {
        Order order = Order.builder()
                .orderNumber("ORD-20240101-ABCD1234")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000"))
                .build();

        OrderItem orderItem = OrderItem.builder()
                .productId(456L)
                .skuId(789L)
                .productName("테스트 상품(빨강, L)")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000"))
                .totalPrice(new BigDecimal("50000"))
                .build();

        order.addOrderItem(orderItem);

        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();
        order.setOrderDelivery(orderDelivery);

        return order;
    }

    @Test
    @DisplayName("주문 생성 성공 - 상품명에 옵션명 포함")
    void createOrder_Success() {
        // given
        given(productServiceClient.getProductDetail(eq(456L))).willReturn(productDetailResponse);
        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderResponse response = orderService.createOrder(1L, createRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-ABCD1234");
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        verify(productServiceClient).getProductDetail(456L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 - 여러 상품 주문")
    void createOrder_MultipleItems() {
        // given
        OrderItemRequest item1 = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .quantity(2)
                .build();

        OrderItemRequest item2 = OrderItemRequest.builder()
                .productId(101L)
                .skuId(1002L)
                .quantity(1)
                .build();

        OrderCreateRequest multiItemRequest = OrderCreateRequest.builder()
                .orderItems(List.of(item1, item2))
                .deliveryInfo(deliveryInfoRequest)
                .build();

        ProductDetailResponse product1 = ProductDetailResponse.builder()
                .productId(100L)
                .productName("상품1")
                .productCode("PROD-100")
                .skus(List.of(SkuResponse.builder()
                        .id(1001L)
                        .price(10000L)
                        .optionValueIds(List.of())
                        .build()))
                .optionGroups(List.of())
                .build();

        ProductDetailResponse product2 = ProductDetailResponse.builder()
                .productId(101L)
                .productName("상품2")
                .productCode("PROD-101")
                .skus(List.of(SkuResponse.builder()
                        .id(1002L)
                        .price(30000L)
                        .optionValueIds(List.of())
                        .build()))
                .optionGroups(List.of())
                .build();

        Order multiItemOrder = Order.builder()
                .orderNumber("ORD-20240101-MULTI123")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000"))
                .totalDiscountAmount(BigDecimal.ZERO)
                .totalPaymentAmount(new BigDecimal("50000"))
                .build();
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(100L).skuId(1001L).productName("상품1")
                .quantity(2).unitPrice(new BigDecimal("10000"))
                .totalPrice(new BigDecimal("20000")).build());
        multiItemOrder.addOrderItem(OrderItem.builder()
                .productId(101L).skuId(1002L).productName("상품2")
                .quantity(1).unitPrice(new BigDecimal("30000"))
                .totalPrice(new BigDecimal("30000")).build());

        OrderDelivery orderDelivery = OrderDelivery.builder()
                .receiverName("홍길동")
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .addressDetail("아파트 101동 202호")
                .deliveryMemo("문 앞에 놓아주세요.")
                .build();
        multiItemOrder.setOrderDelivery(orderDelivery);

        given(productServiceClient.getProductDetail(100L)).willReturn(product1);
        given(productServiceClient.getProductDetail(101L)).willReturn(product2);
        given(orderRepository.save(any(Order.class))).willReturn(multiItemOrder);

        // when
        OrderResponse response = orderService.createOrder(1L, multiItemRequest);

        // then
        assertThat(response.getOrderNumber()).isEqualTo("ORD-20240101-MULTI123");
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        verify(productServiceClient).getProductDetail(100L);
        verify(productServiceClient).getProductDetail(101L);
    }
}

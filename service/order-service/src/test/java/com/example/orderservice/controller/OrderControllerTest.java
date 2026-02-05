package com.example.orderservice.controller;

import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse orderResponse;
    private OrderCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        OrderItemResponse itemResponse = OrderItemResponse.builder()
                .orderItemId(1L)
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .totalPrice(new BigDecimal("50000.00"))
                .build();

        orderResponse = OrderResponse.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-ABCD1234")
                .userId(1L)
                .orderStatus(OrderStatus.CREATED)
                .totalProductAmount(new BigDecimal("50000.00"))
                .totalDiscountAmount(new BigDecimal("5000.00"))
                .totalPaymentAmount(new BigDecimal("45000.00"))
                .orderMemo("테스트 주문")
                .orderedAt(LocalDateTime.now())
                .orderItems(List.of(itemResponse))
                .build();

        OrderItemRequest itemRequest = OrderItemRequest.builder()
                .productId(100L)
                .skuId(1001L)
                .productName("테스트 상품")
                .productCode("PROD-001")
                .quantity(2)
                .unitPrice(new BigDecimal("25000.00"))
                .build();

        createRequest = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of(itemRequest))
                .discountAmount(new BigDecimal("5000.00"))
                .orderMemo("테스트 주문")
                .build();
    }

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공")
    void createOrder_Success() throws Exception {
        // given
        given(orderService.createOrder(any(OrderCreateRequest.class))).willReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20240101-ABCD1234"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.totalProductAmount").value(50000.00))
                .andExpect(jsonPath("$.totalDiscountAmount").value(5000.00))
                .andExpect(jsonPath("$.totalPaymentAmount").value(45000.00))
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems[0].productName").value("테스트 상품"));
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (userId 누락)")
    void createOrder_ValidationFail_MissingUserId() throws Exception {
        // given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder()
                        .productId(100L)
                        .skuId(1001L)
                        .productName("상품")
                        .quantity(1)
                        .unitPrice(new BigDecimal("10000"))
                        .build()))
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (주문 상품 없음)")
    void createOrder_ValidationFail_EmptyOrderItems() throws Exception {
        // given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of())
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (수량 0 이하)")
    void createOrder_ValidationFail_InvalidQuantity() throws Exception {
        // given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .userId(1L)
                .orderItems(List.of(OrderItemRequest.builder()
                        .productId(100L)
                        .skuId(1001L)
                        .productName("상품")
                        .quantity(0)
                        .unitPrice(new BigDecimal("10000"))
                        .build()))
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}

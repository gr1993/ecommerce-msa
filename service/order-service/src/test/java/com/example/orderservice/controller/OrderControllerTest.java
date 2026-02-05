package com.example.orderservice.controller;

import com.example.orderservice.domain.entity.OrderStatus;
import com.example.orderservice.dto.request.DeliveryInfoRequest;
import com.example.orderservice.dto.request.OrderCreateRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private DeliveryInfoRequest deliveryInfoRequest;

    @BeforeEach
    void setUp() {
        orderResponse = OrderResponse.builder()
                .orderId(1L)
                .orderNumber("ORD-20240101-ABCD1234")
                .orderStatus(OrderStatus.CREATED)
                .orderedAt(LocalDateTime.now())
                .build();

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
    }

    @Test
    @DisplayName("POST /api/orders - 주문 생성 성공")
    void createOrder_Success() throws Exception {
        // given
        given(orderService.createOrder(eq(1L), any(OrderCreateRequest.class))).willReturn(orderResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-20240101-ABCD1234"))
                .andExpect(jsonPath("$.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.orderedAt").exists());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (X-User-Id 헤더 누락)")
    void createOrder_ValidationFail_MissingUserIdHeader() throws Exception {
        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (주문 상품 없음)")
    void createOrder_ValidationFail_EmptyOrderItems() throws Exception {
        // given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of())
                .deliveryInfo(deliveryInfoRequest)
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
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
                .orderItems(List.of(OrderItemRequest.builder()
                        .productId(456L)
                        .skuId(789L)
                        .quantity(0)
                        .build()))
                .deliveryInfo(deliveryInfoRequest)
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (배송 정보 누락)")
    void createOrder_ValidationFail_MissingDeliveryInfo() throws Exception {
        // given
        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder()
                        .productId(456L)
                        .skuId(789L)
                        .quantity(2)
                        .build()))
                .deliveryInfo(null)
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 유효성 검증 실패 (수령인 이름 누락)")
    void createOrder_ValidationFail_MissingReceiverName() throws Exception {
        // given
        DeliveryInfoRequest invalidDeliveryInfo = DeliveryInfoRequest.builder()
                .receiverPhone("010-1234-5678")
                .zipcode("12345")
                .address("서울특별시 강남구 테헤란로 123")
                .build();

        OrderCreateRequest invalidRequest = OrderCreateRequest.builder()
                .orderItems(List.of(OrderItemRequest.builder()
                        .productId(456L)
                        .skuId(789L)
                        .quantity(2)
                        .build()))
                .deliveryInfo(invalidDeliveryInfo)
                .build();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}

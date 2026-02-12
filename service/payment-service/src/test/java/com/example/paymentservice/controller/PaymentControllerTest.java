package com.example.paymentservice.controller;

import com.example.paymentservice.dto.request.PaymentConfirmRequest;
import com.example.paymentservice.dto.response.PaymentConfirmResponse;
import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentService paymentService;

	@Test
	@DisplayName("결제 승인 요청 성공")
	void confirmPayment_Success() throws Exception {
		// given
		PaymentConfirmRequest request = new PaymentConfirmRequest(
				"test-payment-key",
				"ORDER-001",
				10000L
		);

		PaymentConfirmResponse response = PaymentConfirmResponse.builder()
				.orderNumber("ORDER-001")
				.paymentKey("test-payment-key")
				.amount(10000L)
				.status("DONE")
				.approvedAt("2026-02-09T10:00:00+09:00")
				.build();

		given(paymentService.confirmPayment(any(PaymentConfirmRequest.class)))
				.willReturn(response);

		// when & then
		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderNumber").value("ORDER-001"))
				.andExpect(jsonPath("$.paymentKey").value("test-payment-key"))
				.andExpect(jsonPath("$.amount").value(10000))
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.approvedAt").value("2026-02-09T10:00:00+09:00"));

		verify(paymentService).confirmPayment(any(PaymentConfirmRequest.class));
	}

	@Test
	@DisplayName("결제 승인 요청 실패 - 주문을 찾을 수 없음")
	void confirmPayment_OrderNotFound() throws Exception {
		// given
		PaymentConfirmRequest request = new PaymentConfirmRequest(
				"test-payment-key",
				"NON-EXISTENT-ORDER",
				10000L
		);

		given(paymentService.confirmPayment(any(PaymentConfirmRequest.class)))
				.willThrow(new IllegalArgumentException("주문을 찾을 수 없습니다. orderNumber: NON-EXISTENT-ORDER"));

		// when & then
		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("결제 승인 요청 실패 - 금액 불일치")
	void confirmPayment_AmountMismatch() throws Exception {
		// given
		PaymentConfirmRequest request = new PaymentConfirmRequest(
				"test-payment-key",
				"ORDER-001",
				99999L  // 잘못된 금액
		);

		given(paymentService.confirmPayment(any(PaymentConfirmRequest.class)))
				.willThrow(new IllegalArgumentException("결제 금액이 일치하지 않습니다."));

		// when & then
		mockMvc.perform(post("/api/payments/confirm")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andDo(print())
				.andExpect(status().isBadRequest());
	}
}

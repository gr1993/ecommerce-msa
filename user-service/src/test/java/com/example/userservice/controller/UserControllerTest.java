package com.example.userservice.controller;

import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("정상 회원가입 API 테스트")
	void testSignUpSuccess() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		SignUpResponse response = new SignUpResponse(
				1L,
				"test@example.com",
				"홍길동",
				"010-1234-5678",
				LocalDateTime.now()
		);

		when(userService.signUp(any(SignUpRequest.class))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1L))
				.andExpect(jsonPath("$.email").value("test@example.com"))
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.phone").value("010-1234-5678"))
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("이메일 형식 오류 시 400 에러 반환 테스트")
	void testSignUpWithInvalidEmail() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("invalid-email");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비밀번호 형식 오류 시 400 에러 반환 테스트")
	void testSignUpWithInvalidPassword() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("weak");
		request.setPasswordConfirm("weak");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("필수 필드 누락 시 400 에러 반환 테스트")
	void testSignUpWithMissingFields() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		// name 필드 누락

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("연락처 형식 오류 시 400 에러 반환 테스트")
	void testSignUpWithInvalidPhone() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("123-456-789"); // 잘못된 형식

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("연락처 없이 회원가입 API 테스트")
	void testSignUpWithoutPhone() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone(null);

		SignUpResponse response = new SignUpResponse(
				1L,
				"test@example.com",
				"홍길동",
				null,
				LocalDateTime.now()
		);

		when(userService.signUp(any(SignUpRequest.class))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(1L))
				.andExpect(jsonPath("$.email").value("test@example.com"))
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.phone").isEmpty());
	}
}


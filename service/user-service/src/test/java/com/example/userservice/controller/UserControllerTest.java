package com.example.userservice.controller;

import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.dto.response.UserResponse;
import com.example.userservice.exception.UserNotFoundException;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

	@Test
	@DisplayName("전체 회원 목록 조회 API 테스트")
	void testGetAllUsers() throws Exception {
		// given
		LocalDateTime now = LocalDateTime.now();

		UserResponse user1 = UserResponse.builder()
				.userId(1L)
				.email("user1@example.com")
				.name("홍길동")
				.phone("010-1234-5678")
				.status("ACTIVE")
				.grade("VIP")
				.points(5000L)
				.createdAt(now)
				.updatedAt(now)
				.build();

		UserResponse user2 = UserResponse.builder()
				.userId(2L)
				.email("user2@example.com")
				.name("김철수")
				.phone("010-2345-6789")
				.status("ACTIVE")
				.grade("NORMAL")
				.points(1200L)
				.createdAt(now)
				.updatedAt(now)
				.build();

		List<UserResponse> users = Arrays.asList(user1, user2);
		when(userService.getAllUsers()).thenReturn(users);

		// when & then
		mockMvc.perform(get("/api/users")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].userId").value(1L))
				.andExpect(jsonPath("$[0].email").value("user1@example.com"))
				.andExpect(jsonPath("$[0].name").value("홍길동"))
				.andExpect(jsonPath("$[0].phone").value("010-1234-5678"))
				.andExpect(jsonPath("$[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$[0].grade").value("VIP"))
				.andExpect(jsonPath("$[0].points").value(5000))
				.andExpect(jsonPath("$[1].userId").value(2L))
				.andExpect(jsonPath("$[1].email").value("user2@example.com"))
				.andExpect(jsonPath("$[1].name").value("김철수"))
				.andExpect(jsonPath("$[1].grade").value("NORMAL"))
				.andExpect(jsonPath("$[1].points").value(1200));
	}

	@Test
	@DisplayName("빈 회원 목록 조회 API 테스트")
	void testGetAllUsersEmpty() throws Exception {
		// given
		when(userService.getAllUsers()).thenReturn(Arrays.asList());

		// when & then
		mockMvc.perform(get("/api/users")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("특정 회원 정보 조회 API 성공 테스트")
	void testGetUserByIdSuccess() throws Exception {
		// given
		Long userId = 1L;
		LocalDateTime now = LocalDateTime.now();

		UserResponse user = UserResponse.builder()
				.userId(userId)
				.email("user@example.com")
				.name("홍길동")
				.phone("010-1234-5678")
				.status("ACTIVE")
				.grade("GOLD")
				.points(3000L)
				.createdAt(now)
				.updatedAt(now)
				.build();

		when(userService.getUserById(userId)).thenReturn(user);

		// when & then
		mockMvc.perform(get("/api/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1L))
				.andExpect(jsonPath("$.email").value("user@example.com"))
				.andExpect(jsonPath("$.name").value("홍길동"))
				.andExpect(jsonPath("$.phone").value("010-1234-5678"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.grade").value("GOLD"))
				.andExpect(jsonPath("$.points").value(3000))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("존재하지 않는 회원 조회 시 404 에러 반환 테스트")
	void testGetUserByIdNotFound() throws Exception {
		// given
		Long userId = 999L;
		when(userService.getUserById(userId))
				.thenThrow(new UserNotFoundException("사용자를 찾을 수 없습니다. userId: " + userId));

		// when & then
		mockMvc.perform(get("/api/users/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("사용자를 찾을 수 없습니다. userId: " + userId));
	}
}


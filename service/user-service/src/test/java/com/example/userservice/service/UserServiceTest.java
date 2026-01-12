package com.example.userservice.service;

import com.example.userservice.common.EventTypeConstants;
import com.example.userservice.domain.entity.Outbox;
import com.example.userservice.domain.entity.User;
import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.dto.response.UserResponse;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.PasswordMismatchException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.OutboxRepository;
import com.example.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private OutboxRepository outboxRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private UserService userService;

	@Test
	@DisplayName("정상 회원가입 테스트")
	void testSignUpSuccess() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		LocalDateTime createdAt = LocalDateTime.now();
		String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\",\"name\":\"홍길동\",\"phone\":\"010-1234-5678\",\"registeredAt\":\"" + createdAt + "\"}";

		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			User savedUser = User.builder()
					.email(user.getEmail())
					.password(user.getPassword())
					.name(user.getName())
					.phone(user.getPhone())
					.build();
			ReflectionTestUtils.setField(savedUser, "userId", 1L);
			ReflectionTestUtils.setField(savedUser, "createdAt", createdAt);
			return savedUser;
		});
		when(objectMapper.writeValueAsString(any())).thenReturn(eventPayload);
		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		SignUpResponse response = userService.signUp(request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getEmail()).isEqualTo("test@example.com");
		assertThat(response.getName()).isEqualTo("홍길동");
		assertThat(response.getPhone()).isEqualTo("010-1234-5678");
		assertThat(response.getUserId()).isNotNull();
		assertThat(response.getCreatedAt()).isNotNull();

		verify(userRepository, times(1)).existsByEmail("test@example.com");
		verify(passwordEncoder, times(1)).encode("password123!");
		verify(userRepository, times(1)).save(any(User.class));
		verify(objectMapper, times(1)).writeValueAsString(any());
		
		// Outbox 저장 검증
		ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
		verify(outboxRepository, times(1)).save(outboxCaptor.capture());
		
		Outbox savedOutbox = outboxCaptor.getValue();
		assertThat(savedOutbox.getAggregateType()).isEqualTo("User");
		assertThat(savedOutbox.getAggregateId()).isEqualTo("1");
		assertThat(savedOutbox.getEventType()).isEqualTo(EventTypeConstants.TOPIC_USER_REGISTERED);
		assertThat(savedOutbox.getPayload()).isEqualTo(eventPayload);
		assertThat(savedOutbox.getStatus()).isEqualTo(Outbox.OutboxStatus.PENDING);
	}

	@Test
	@DisplayName("이메일 중복 시 예외 발생 테스트")
	void testSignUpWithDuplicateEmail() {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("duplicate@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> userService.signUp(request))
				.isInstanceOf(DuplicateEmailException.class)
				.hasMessage("이미 사용 중인 이메일입니다.");

		verify(userRepository, times(1)).existsByEmail("duplicate@example.com");
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("비밀번호 불일치 시 예외 발생 테스트")
	void testSignUpWithPasswordMismatch() {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("differentPassword!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> userService.signUp(request))
				.isInstanceOf(PasswordMismatchException.class)
				.hasMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");

		verify(userRepository, times(1)).existsByEmail("test@example.com");
		verify(passwordEncoder, never()).encode(anyString());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	@DisplayName("연락처 없이 회원가입 테스트")
	void testSignUpWithoutPhone() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone(null);

		LocalDateTime createdAt = LocalDateTime.now();
		String eventPayload = "{\"userId\":1,\"email\":\"test@example.com\",\"name\":\"홍길동\",\"phone\":null,\"registeredAt\":\"" + createdAt + "\"}";

		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			User savedUser = User.builder()
					.email(user.getEmail())
					.password(user.getPassword())
					.name(user.getName())
					.phone(user.getPhone())
					.build();
			ReflectionTestUtils.setField(savedUser, "userId", 1L);
			ReflectionTestUtils.setField(savedUser, "createdAt", createdAt);
			return savedUser;
		});
		when(objectMapper.writeValueAsString(any())).thenReturn(eventPayload);
		when(outboxRepository.save(any(Outbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// when
		SignUpResponse response = userService.signUp(request);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getEmail()).isEqualTo("test@example.com");
		assertThat(response.getName()).isEqualTo("홍길동");
		assertThat(response.getPhone()).isNull();

		// Outbox 저장 검증
		verify(outboxRepository, times(1)).save(any(Outbox.class));
	}

	@Test
	@DisplayName("이벤트 직렬화 실패 시 예외 발생 테스트")
	void testSignUpWithJsonProcessingException() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest();
		request.setEmail("test@example.com");
		request.setPassword("password123!");
		request.setPasswordConfirm("password123!");
		request.setName("홍길동");
		request.setPhone("010-1234-5678");

		LocalDateTime createdAt = LocalDateTime.now();

		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			User savedUser = User.builder()
					.email(user.getEmail())
					.password(user.getPassword())
					.name(user.getName())
					.phone(user.getPhone())
					.build();
			ReflectionTestUtils.setField(savedUser, "userId", 1L);
			ReflectionTestUtils.setField(savedUser, "createdAt", createdAt);
			return savedUser;
		});
		when(objectMapper.writeValueAsString(any())).thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("직렬화 실패") {});

		// when & then
		assertThatThrownBy(() -> userService.signUp(request))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("이벤트 저장 중 오류가 발생했습니다.");

		verify(userRepository, times(1)).save(any(User.class));
		verify(objectMapper, times(1)).writeValueAsString(any());
		verify(outboxRepository, never()).save(any(Outbox.class));
	}

	@Test
	@DisplayName("전체 회원 목록 조회 테스트")
	void testGetAllUsers() {
		// given
		LocalDateTime now = LocalDateTime.now();

		User user1 = User.builder()
				.email("user1@example.com")
				.password("encodedPassword1")
				.name("홍길동")
				.phone("010-1234-5678")
				.status(User.UserStatus.ACTIVE)
				.grade(User.UserGrade.VIP)
				.points(5000L)
				.build();
		ReflectionTestUtils.setField(user1, "userId", 1L);
		ReflectionTestUtils.setField(user1, "createdAt", now);
		ReflectionTestUtils.setField(user1, "updatedAt", now);

		User user2 = User.builder()
				.email("user2@example.com")
				.password("encodedPassword2")
				.name("김철수")
				.phone("010-2345-6789")
				.status(User.UserStatus.ACTIVE)
				.grade(User.UserGrade.NORMAL)
				.points(1200L)
				.build();
		ReflectionTestUtils.setField(user2, "userId", 2L);
		ReflectionTestUtils.setField(user2, "createdAt", now);
		ReflectionTestUtils.setField(user2, "updatedAt", now);

		List<User> users = Arrays.asList(user1, user2);
		when(userRepository.findAll()).thenReturn(users);

		// when
		List<UserResponse> responses = userService.getAllUsers();

		// then
		assertThat(responses).hasSize(2);

		UserResponse response1 = responses.get(0);
		assertThat(response1.getUserId()).isEqualTo(1L);
		assertThat(response1.getEmail()).isEqualTo("user1@example.com");
		assertThat(response1.getName()).isEqualTo("홍길동");
		assertThat(response1.getPhone()).isEqualTo("010-1234-5678");
		assertThat(response1.getStatus()).isEqualTo("ACTIVE");
		assertThat(response1.getGrade()).isEqualTo("VIP");
		assertThat(response1.getPoints()).isEqualTo(5000L);

		UserResponse response2 = responses.get(1);
		assertThat(response2.getUserId()).isEqualTo(2L);
		assertThat(response2.getEmail()).isEqualTo("user2@example.com");
		assertThat(response2.getName()).isEqualTo("김철수");
		assertThat(response2.getGrade()).isEqualTo("NORMAL");
		assertThat(response2.getPoints()).isEqualTo(1200L);

		verify(userRepository, times(1)).findAll();
	}

	@Test
	@DisplayName("특정 회원 정보 조회 성공 테스트")
	void testGetUserByIdSuccess() {
		// given
		LocalDateTime now = LocalDateTime.now();
		Long userId = 1L;

		User user = User.builder()
				.email("user@example.com")
				.password("encodedPassword")
				.name("홍길동")
				.phone("010-1234-5678")
				.status(User.UserStatus.ACTIVE)
				.grade(User.UserGrade.GOLD)
				.points(3000L)
				.build();
		ReflectionTestUtils.setField(user, "userId", userId);
		ReflectionTestUtils.setField(user, "createdAt", now);
		ReflectionTestUtils.setField(user, "updatedAt", now);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		// when
		UserResponse response = userService.getUserById(userId);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getUserId()).isEqualTo(userId);
		assertThat(response.getEmail()).isEqualTo("user@example.com");
		assertThat(response.getName()).isEqualTo("홍길동");
		assertThat(response.getPhone()).isEqualTo("010-1234-5678");
		assertThat(response.getStatus()).isEqualTo("ACTIVE");
		assertThat(response.getGrade()).isEqualTo("GOLD");
		assertThat(response.getPoints()).isEqualTo(3000L);
		assertThat(response.getCreatedAt()).isEqualTo(now);
		assertThat(response.getUpdatedAt()).isEqualTo(now);

		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("존재하지 않는 회원 조회 시 예외 발생 테스트")
	void testGetUserByIdNotFound() {
		// given
		Long userId = 999L;
		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userService.getUserById(userId))
				.isInstanceOf(UserNotFoundException.class)
				.hasMessageContaining("사용자를 찾을 수 없습니다. userId: " + userId);

		verify(userRepository, times(1)).findById(userId);
	}

	@Test
	@DisplayName("빈 회원 목록 조회 테스트")
	void testGetAllUsersEmpty() {
		// given
		when(userRepository.findAll()).thenReturn(Arrays.asList());

		// when
		List<UserResponse> responses = userService.getAllUsers();

		// then
		assertThat(responses).isEmpty();
		verify(userRepository, times(1)).findAll();
	}
}


package com.example.userservice.service;

import com.example.userservice.common.EventTypeConstants;
import com.example.userservice.domain.entity.Outbox;
import com.example.userservice.domain.entity.User;
import com.example.userservice.domain.event.UserRegisteredEvent;
import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.dto.response.UserResponse;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.PasswordMismatchException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.OutboxRepository;
import com.example.userservice.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final OutboxRepository outboxRepository;
	private final PasswordEncoder passwordEncoder;
	private final ObjectMapper objectMapper;

	@Transactional
	public SignUpResponse signUp(SignUpRequest request) {
		// 이메일 중복 체크
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
		}

		// 비밀번호 확인 검증
		if (!request.getPassword().equals(request.getPasswordConfirm())) {
			throw new PasswordMismatchException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
		}

		// 비밀번호 암호화
		String encodedPassword = passwordEncoder.encode(request.getPassword());

		// 사용자 생성 (role은 무조건 USER로 설정, 관리자는 DB에서 수동 변경)
		User user = User.builder()
				.email(request.getEmail())
				.password(encodedPassword)
				.name(request.getName())
				.phone(request.getPhone())
				.role(User.UserRole.USER)
				.build();

		User savedUser = userRepository.save(user);

		// 이벤트 생성 및 Outbox에 저장
		saveUserRegisteredEvent(savedUser);

		return new SignUpResponse(
				savedUser.getUserId(),
				savedUser.getEmail(),
				savedUser.getName(),
				savedUser.getPhone(),
				savedUser.getCreatedAt()
		);
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getAllUsers() {
		List<User> users = userRepository.findAll();
		return users.stream()
				.map(UserResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public UserResponse getUserById(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. userId: " + userId));
		return UserResponse.from(user);
	}

	private void saveUserRegisteredEvent(User user) {
		try {
			UserRegisteredEvent event = new UserRegisteredEvent(
					user.getUserId(),
					user.getEmail(),
					user.getName(),
					user.getPhone(),
					user.getRole().name(),
					user.getPassword(),
					user.getCreatedAt()
			);

			String payload = objectMapper.writeValueAsString(event);

			Outbox outbox = Outbox.builder()
					.aggregateType("User")
					.aggregateId(String.valueOf(user.getUserId()))
					.eventType(EventTypeConstants.TOPIC_USER_REGISTERED)
					.payload(payload)
					.build();

			outboxRepository.save(outbox);
			log.info("UserRegistered 이벤트가 Outbox에 저장되었습니다. userId: {}", user.getUserId());
		} catch (JsonProcessingException e) {
			log.error("이벤트 직렬화 실패: userId={}", user.getUserId(), e);
			throw new RuntimeException("이벤트 저장 중 오류가 발생했습니다.", e);
		}
	}
}



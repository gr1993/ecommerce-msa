package com.example.userservice.service;

import com.example.userservice.domain.entity.User;
import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.exception.DuplicateEmailException;
import com.example.userservice.exception.PasswordMismatchException;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

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

		// 사용자 생성
		User user = User.builder()
				.email(request.getEmail())
				.password(encodedPassword)
				.name(request.getName())
				.phone(request.getPhone())
				.build();

		User savedUser = userRepository.save(user);

		return new SignUpResponse(
				savedUser.getUserId(),
				savedUser.getEmail(),
				savedUser.getName(),
				savedUser.getPhone(),
				savedUser.getCreatedAt()
		);
	}
}



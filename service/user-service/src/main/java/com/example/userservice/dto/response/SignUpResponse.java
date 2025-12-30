package com.example.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "회원가입 응답 DTO")
public class SignUpResponse {
	@Schema(description = "사용자 ID", example = "1")
	private Long userId;
	
	@Schema(description = "이메일 주소", example = "user@example.com")
	private String email;
	
	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;
	
	@Schema(description = "연락처", example = "010-1234-5678")
	private String phone;
	
	@Schema(description = "생성 일시", example = "2024-01-01T12:00:00")
	private LocalDateTime createdAt;
}


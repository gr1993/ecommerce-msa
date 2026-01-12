package com.example.userservice.dto.response;

import com.example.userservice.domain.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "회원 정보 응답 DTO")
public class UserResponse {
	@Schema(description = "사용자 ID", example = "1")
	private Long userId;

	@Schema(description = "이메일 주소", example = "user@example.com")
	private String email;

	@Schema(description = "사용자 이름", example = "홍길동")
	private String name;

	@Schema(description = "연락처", example = "010-1234-5678")
	private String phone;

	@Schema(description = "사용자 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
	private String status;

	@Schema(description = "회원 등급", example = "NORMAL", allowableValues = {"NORMAL", "VIP", "GOLD", "SILVER"})
	private String grade;

	@Schema(description = "포인트", example = "1000")
	private Long points;

	@Schema(description = "생성 일시", example = "2024-01-01T12:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "수정 일시", example = "2024-01-15T10:00:00")
	private LocalDateTime updatedAt;

	public static UserResponse from(User user) {
		return UserResponse.builder()
			.userId(user.getUserId())
			.email(user.getEmail())
			.name(user.getName())
			.phone(user.getPhone())
			.status(user.getStatus().name())
			.grade(user.getGrade().name())
			.points(user.getPoints())
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}
}

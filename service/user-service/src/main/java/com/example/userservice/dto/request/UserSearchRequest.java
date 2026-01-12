package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원 검색 요청 DTO")
public class UserSearchRequest {
	@Schema(description = "검색어 (이메일 또는 이름)", example = "홍길동")
	private String searchText;

	@Schema(description = "사용자 상태 필터", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
	private String status;

	@Schema(description = "회원 등급 필터", example = "VIP", allowableValues = {"NORMAL", "VIP", "GOLD", "SILVER"})
	private String grade;
}

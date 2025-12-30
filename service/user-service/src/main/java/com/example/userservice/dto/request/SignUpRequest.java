package com.example.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class SignUpRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	@Schema(description = "이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
			message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
	@Schema(description = "비밀번호 (최소 8자, 영문/숫자/특수문자 포함)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
	private String password;

	@NotBlank(message = "비밀번호 확인은 필수입니다.")
	@Schema(description = "비밀번호 확인", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
	private String passwordConfirm;

	@NotBlank(message = "이름은 필수입니다.")
	@Schema(description = "사용자 이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
	private String name;

	@Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", 
			message = "올바른 연락처 형식이 아닙니다. (예: 010-1234-5678)")
	@Schema(description = "연락처 (예: 010-1234-5678)", example = "010-1234-5678")
	private String phone;
}


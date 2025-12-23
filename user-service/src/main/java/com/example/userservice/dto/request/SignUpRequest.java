package com.example.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "올바른 이메일 형식이 아닙니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", 
			message = "비밀번호는 최소 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
	private String password;

	@NotBlank(message = "비밀번호 확인은 필수입니다.")
	private String passwordConfirm;

	@NotBlank(message = "이름은 필수입니다.")
	private String name;

	@Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$", 
			message = "올바른 연락처 형식이 아닙니다. (예: 010-1234-5678)")
	private String phone;
}


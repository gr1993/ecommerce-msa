package com.example.userservice.controller;

import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

	private final UserService userService;

	@PostMapping("/signup")
	@Operation(
		summary = "회원가입",
		description = "새로운 사용자를 등록합니다. 이메일, 비밀번호, 이름, 연락처 정보가 필요합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "201",
			description = "회원가입 성공",
			content = @Content(schema = @Schema(implementation = SignUpResponse.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 (유효성 검증 실패 또는 비밀번호 불일치)"
		),
		@ApiResponse(
			responseCode = "409",
			description = "이메일 중복"
		)
	})
	public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		SignUpResponse response = userService.signUp(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}


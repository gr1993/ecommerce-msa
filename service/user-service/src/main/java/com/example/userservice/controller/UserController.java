package com.example.userservice.controller;

import com.example.userservice.dto.request.SignUpRequest;
import com.example.userservice.dto.response.SignUpResponse;
import com.example.userservice.dto.response.UserResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

	@GetMapping
	@Operation(
		summary = "전체 회원 목록 조회",
		description = "등록된 모든 사용자의 목록을 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = UserResponse.class))
		)
	})
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		List<UserResponse> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@GetMapping("/{userId}")
	@Operation(
		summary = "특정 회원 정보 조회",
		description = "사용자 ID로 특정 회원의 정보를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = UserResponse.class))
		),
		@ApiResponse(
			responseCode = "404",
			description = "사용자를 찾을 수 없음"
		)
	})
	public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
		UserResponse user = userService.getUserById(userId);
		return ResponseEntity.ok(user);
	}
}


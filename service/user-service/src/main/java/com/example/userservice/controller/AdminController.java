package com.example.userservice.controller;

import com.example.userservice.dto.request.UserSearchRequest;
import com.example.userservice.dto.response.PageResponse;
import com.example.userservice.dto.response.UserResponse;
import com.example.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 API")
public class AdminController {

	private final UserService userService;

	@GetMapping
	@Operation(
		summary = "회원 목록 조회 (페이지네이션 + 검색)",
		description = "페이지네이션과 검색 필터를 적용하여 사용자 목록을 조회합니다. (관리자 전용)"
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(schema = @Schema(implementation = PageResponse.class))
		)
	})
	public ResponseEntity<PageResponse<UserResponse>> searchUsers(
			@RequestParam(name = "searchText", required = false) String searchText,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "grade", required = false) String grade,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size,
			@RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
			@RequestParam(name = "sortDirection", defaultValue = "DESC") String sortDirection
	) {
		UserSearchRequest searchRequest = new UserSearchRequest(searchText, status, grade);

		Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

		PageResponse<UserResponse> users = userService.searchUsers(searchRequest, pageable);
		return ResponseEntity.ok(users);
	}

	@GetMapping("/all")
	@Operation(
		summary = "전체 회원 목록 조회 (페이지네이션 없음)",
		description = "등록된 모든 사용자의 목록을 조회합니다. (관리자 전용)"
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
		description = "사용자 ID로 특정 회원의 정보를 조회합니다. (관리자 전용)"
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
	public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") Long userId) {
		UserResponse user = userService.getUserById(userId);
		return ResponseEntity.ok(user);
	}
}

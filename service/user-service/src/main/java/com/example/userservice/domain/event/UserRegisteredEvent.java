package com.example.userservice.domain.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
	private Long userId;
	private String email;
	private String name;
	private String phone;
	private String role;
	private String hashedPassword;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
	@Schema(description = "생성 일시", example = "2026-01-23T16:58:34.035882", type = "string")
	private LocalDateTime registeredAt;
}


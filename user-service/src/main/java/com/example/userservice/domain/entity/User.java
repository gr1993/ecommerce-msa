package com.example.userservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password", nullable = false, length = 255)
	private String password;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "phone", length = 20)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Enumerated(EnumType.STRING)
	@Column(name = "grade", nullable = false, length = 20)
	private UserGrade grade = UserGrade.NORMAL;

	@Column(name = "points", nullable = false)
	private Long points = 0L;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Builder
	public User(String email, String password, String name, String phone, UserStatus status, UserGrade grade, Long points) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.status = status != null ? status : UserStatus.ACTIVE;
		this.grade = grade != null ? grade : UserGrade.NORMAL;
		this.points = points != null ? points : 0L;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updatePhone(String phone) {
		this.phone = phone;
	}

	public void updateStatus(UserStatus status) {
		this.status = status;
	}

	public void updateGrade(UserGrade grade) {
		this.grade = grade;
	}

	public void addPoints(Long points) {
		this.points += points;
	}

	public void subtractPoints(Long points) {
		if (this.points < points) {
			throw new IllegalArgumentException("포인트가 부족합니다.");
		}
		this.points -= points;
	}

	public enum UserStatus {
		ACTIVE, INACTIVE, SUSPENDED
	}

	public enum UserGrade {
		NORMAL, VIP
	}
}


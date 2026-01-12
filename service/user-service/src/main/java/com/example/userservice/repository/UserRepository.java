package com.example.userservice.repository;

import com.example.userservice.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	@Query("SELECT u FROM User u WHERE " +
			"(:searchText IS NULL OR :searchText = '' OR " +
			"LOWER(u.email) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
			"LOWER(u.name) LIKE LOWER(CONCAT('%', :searchText, '%'))) AND " +
			"(:status IS NULL OR u.status = :status) AND " +
			"(:grade IS NULL OR u.grade = :grade)")
	Page<User> findBySearchCriteria(
			@Param("searchText") String searchText,
			@Param("status") User.UserStatus status,
			@Param("grade") User.UserGrade grade,
			Pageable pageable
	);
}


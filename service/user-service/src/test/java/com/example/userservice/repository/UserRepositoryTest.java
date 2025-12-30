package com.example.userservice.repository;

import com.example.userservice.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("사용자 생성 테스트")
	void testCreateUser() {
		// given
		User user = User.builder()
				.email("test@example.com")
				.password("password123")
				.name("홍길동")
				.phone("010-1234-5678")
				.build();

		// when
		User savedUser = userRepository.save(user);

		// then
		assertThat(savedUser.getUserId()).isNotNull();
		assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
		assertThat(savedUser.getName()).isEqualTo("홍길동");
		assertThat(savedUser.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
		assertThat(savedUser.getGrade()).isEqualTo(User.UserGrade.NORMAL);
		assertThat(savedUser.getPoints()).isEqualTo(0L);
		assertThat(savedUser.getCreatedAt()).isNotNull();
		assertThat(savedUser.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("사용자 조회 테스트")
	void testReadUser() {
		// given
		User user = User.builder()
				.email("read@example.com")
				.password("password123")
				.name("김철수")
				.build();
		User savedUser = userRepository.save(user);

		// when
		Optional<User> foundUser = userRepository.findById(savedUser.getUserId());

		// then
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get().getEmail()).isEqualTo("read@example.com");
		assertThat(foundUser.get().getName()).isEqualTo("김철수");
	}

	@Test
	@DisplayName("이메일로 사용자 조회 테스트")
	void testFindByEmail() {
		// given
		User user = User.builder()
				.email("email@example.com")
				.password("password123")
				.name("이영희")
				.build();
		userRepository.save(user);

		// when
		Optional<User> foundUser = userRepository.findByEmail("email@example.com");

		// then
		assertThat(foundUser).isPresent();
		assertThat(foundUser.get().getEmail()).isEqualTo("email@example.com");
		assertThat(foundUser.get().getName()).isEqualTo("이영희");
	}

	@Test
	@DisplayName("이메일 존재 여부 확인 테스트")
	void testExistsByEmail() {
		// given
		User user = User.builder()
				.email("exists@example.com")
				.password("password123")
				.name("박민수")
				.build();
		userRepository.save(user);

		// when
		boolean exists = userRepository.existsByEmail("exists@example.com");
		boolean notExists = userRepository.existsByEmail("notexists@example.com");

		// then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
	}

	@Test
	@DisplayName("사용자 수정 테스트")
	void testUpdateUser() {
		// given
		User user = User.builder()
				.email("update@example.com")
				.password("password123")
				.name("최수정")
				.phone("010-1111-2222")
				.build();
		User savedUser = userRepository.save(user);

		// when
		savedUser.updateName("최수정(변경)");
		savedUser.updatePhone("010-9999-8888");
		savedUser.updateStatus(User.UserStatus.INACTIVE);
		User updatedUser = userRepository.save(savedUser);

		// then
		assertThat(updatedUser.getName()).isEqualTo("최수정(변경)");
		assertThat(updatedUser.getPhone()).isEqualTo("010-9999-8888");
		assertThat(updatedUser.getStatus()).isEqualTo(User.UserStatus.INACTIVE);
	}

	@Test
	@DisplayName("사용자 삭제 테스트")
	void testDeleteUser() {
		// given
		User user = User.builder()
				.email("delete@example.com")
				.password("password123")
				.name("삭제대상")
				.build();
		User savedUser = userRepository.save(user);
		Long userId = savedUser.getUserId();

		// when
		userRepository.delete(savedUser);

		// then
		Optional<User> deletedUser = userRepository.findById(userId);
		assertThat(deletedUser).isEmpty();
	}

	@Test
	@DisplayName("전체 사용자 조회 테스트")
	void testFindAllUsers() {
		// given
		User user1 = User.builder()
				.email("user1@example.com")
				.password("password123")
				.name("사용자1")
				.build();
		User user2 = User.builder()
				.email("user2@example.com")
				.password("password123")
				.name("사용자2")
				.build();
		userRepository.save(user1);
		userRepository.save(user2);

		// when
		List<User> users = userRepository.findAll();

		// then
		assertThat(users.size()).isGreaterThanOrEqualTo(2);
		assertThat(users).extracting(User::getEmail)
				.contains("user1@example.com", "user2@example.com");
	}

	@Test
	@DisplayName("포인트 추가 테스트")
	void testAddPoints() {
		// given
		User user = User.builder()
				.email("points@example.com")
				.password("password123")
				.name("포인트테스트")
				.points(100L)
				.build();
		User savedUser = userRepository.save(user);

		// when
		savedUser.addPoints(50L);
		User updatedUser = userRepository.save(savedUser);

		// then
		assertThat(updatedUser.getPoints()).isEqualTo(150L);
	}

	@Test
	@DisplayName("포인트 차감 테스트")
	void testSubtractPoints() {
		// given
		User user = User.builder()
				.email("subtract@example.com")
				.password("password123")
				.name("차감테스트")
				.points(100L)
				.build();
		User savedUser = userRepository.save(user);

		// when
		savedUser.subtractPoints(30L);
		User updatedUser = userRepository.save(savedUser);

		// then
		assertThat(updatedUser.getPoints()).isEqualTo(70L);
	}

	@Test
	@DisplayName("등급 변경 테스트")
	void testUpdateGrade() {
		// given
		User user = User.builder()
				.email("grade@example.com")
				.password("password123")
				.name("등급테스트")
				.grade(User.UserGrade.NORMAL)
				.build();
		User savedUser = userRepository.save(user);

		// when
		savedUser.updateGrade(User.UserGrade.VIP);
		User updatedUser = userRepository.save(savedUser);

		// then
		assertThat(updatedUser.getGrade()).isEqualTo(User.UserGrade.VIP);
	}
}


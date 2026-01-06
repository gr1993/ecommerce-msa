package com.example.authservice.repository;

import com.example.authservice.domain.entity.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AuthUserRepository 테스트")
class AuthUserRepositoryTest {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmail_Success() {
        // given
        AuthUser user = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.ACTIVE)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<AuthUser> result = authUserRepository.findByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getPassword()).isEqualTo("encodedPassword");
        assertThat(result.get().getStatus()).isEqualTo(AuthUser.UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("이메일로 사용자 조회 실패 - 존재하지 않는 이메일")
    void findByEmail_NotFound() {
        // when
        Optional<AuthUser> result = authUserRepository.findByEmail("notexist@example.com");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 - 존재함")
    void existsByEmail_True() {
        // given
        AuthUser user = AuthUser.builder()
                .email("existing@example.com")
                .password("encodedPassword")
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        boolean exists = authUserRepository.existsByEmail("existing@example.com");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 - 존재하지 않음")
    void existsByEmail_False() {
        // when
        boolean exists = authUserRepository.existsByEmail("notexist@example.com");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자 저장 성공")
    void save_Success() {
        // given
        AuthUser user = AuthUser.builder()
                .email("newuser@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.ACTIVE)
                .build();

        // when
        AuthUser savedUser = authUserRepository.save(user);

        // then
        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void findById_Success() {
        // given
        AuthUser user = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .build();
        AuthUser savedUser = entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<AuthUser> result = authUserRepository.findById(savedUser.getUserId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(savedUser.getUserId());
    }

    @Test
    @DisplayName("사용자 상태 업데이트 성공")
    void updateStatus_Success() {
        // given
        AuthUser user = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.ACTIVE)
                .build();
        AuthUser savedUser = entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        // when
        AuthUser foundUser = authUserRepository.findById(savedUser.getUserId()).get();
        foundUser.updateStatus(AuthUser.UserStatus.SUSPENDED);
        authUserRepository.save(foundUser);
        entityManager.flush();
        entityManager.clear();

        // then
        AuthUser updatedUser = authUserRepository.findById(savedUser.getUserId()).get();
        assertThat(updatedUser.getStatus()).isEqualTo(AuthUser.UserStatus.SUSPENDED);
    }
}

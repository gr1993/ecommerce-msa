package com.example.authservice.service;

import com.example.authservice.config.JwtProperties;
import com.example.authservice.domain.entity.AuthUser;
import com.example.authservice.domain.event.UserRegisteredEvent;
import com.example.authservice.dto.request.LoginRequest;
import com.example.authservice.dto.request.RefreshTokenRequest;
import com.example.authservice.dto.response.TokenResponse;
import com.example.authservice.exception.*;
import com.example.authservice.repository.AuthUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private AuthUser activeUser;
    private LoginRequest loginRequest;
    private JwtProperties.AccessToken accessTokenProperties;

    @BeforeEach
    void setUp() {
        activeUser = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.ACTIVE)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("rawPassword");

        accessTokenProperties = new JwtProperties.AccessToken();
        accessTokenProperties.setExpiration(1800000L);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // given
        given(authUserRepository.findByEmail(anyString())).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(jwtTokenProvider.createAccessToken(any(AuthUser.class))).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(any(AuthUser.class))).willReturn("refreshToken");
        given(jwtProperties.getAccessToken()).willReturn(accessTokenProperties);

        // when
        TokenResponse response = authService.login(loginRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(1800L);

        verify(authUserRepository, times(1)).findByEmail(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(jwtTokenProvider, times(1)).createAccessToken(any(AuthUser.class));
        verify(jwtTokenProvider, times(1)).createRefreshToken(any(AuthUser.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_UserNotFound() {
        // given
        given(authUserRepository.findByEmail(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(authUserRepository, times(1)).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_Fail_PasswordMismatch() {
        // given
        given(authUserRepository.findByEmail(anyString())).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(authUserRepository, times(1)).findByEmail(anyString());
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 정지된 계정")
    void login_Fail_SuspendedAccount() {
        // given
        AuthUser suspendedUser = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.SUSPENDED)
                .build();
        given(authUserRepository.findByEmail(anyString())).willReturn(Optional.of(suspendedUser));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("정지된 계정입니다.");

        verify(authUserRepository, times(1)).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비활성화된 계정")
    void login_Fail_InactiveAccount() {
        // given
        AuthUser inactiveUser = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.INACTIVE)
                .build();
        given(authUserRepository.findByEmail(anyString())).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("비활성화된 계정입니다.");

        verify(authUserRepository, times(1)).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_Success() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        Claims claims = Jwts.claims().subject("1").build();

        given(jwtTokenProvider.validateRefreshToken(anyString())).willReturn(claims);
        given(authUserRepository.findById(1L)).willReturn(Optional.of(activeUser));
        given(jwtTokenProvider.createAccessToken(any(AuthUser.class))).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(any(AuthUser.class))).willReturn("newRefreshToken");
        given(jwtProperties.getAccessToken()).willReturn(accessTokenProperties);

        // when
        TokenResponse response = authService.refresh(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(jwtTokenProvider, times(1)).validateRefreshToken(anyString());
        verify(authUserRepository, times(1)).findById(1L);
        verify(jwtTokenProvider, times(1)).createAccessToken(any(AuthUser.class));
        verify(jwtTokenProvider, times(1)).createRefreshToken(any(AuthUser.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
    void refresh_Fail_UserNotFound() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        Claims claims = Jwts.claims().subject("999").build();

        given(jwtTokenProvider.validateRefreshToken(anyString())).willReturn(claims);
        given(authUserRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(jwtTokenProvider, times(1)).validateRefreshToken(anyString());
        verify(authUserRepository, times(1)).findById(999L);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 비활성 계정")
    void refresh_Fail_InactiveAccount() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        AuthUser inactiveUser = AuthUser.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .status(AuthUser.UserStatus.INACTIVE)
                .build();

        Claims claims = Jwts.claims().subject("1").build();

        given(jwtTokenProvider.validateRefreshToken(anyString())).willReturn(claims);
        given(authUserRepository.findById(1L)).willReturn(Optional.of(inactiveUser));

        // when & then
        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessage("계정이 활성 상태가 아닙니다.");

        verify(jwtTokenProvider, times(1)).validateRefreshToken(anyString());
        verify(authUserRepository, times(1)).findById(1L);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("이벤트로부터 사용자 등록 성공")
    void registerUserFromEvent_Success() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
                1L,
                "newuser@example.com",
                "홍길동",
                "010-1234-5678",
                "$2a$10$hashedPassword",
                LocalDateTime.now()
        );

        given(authUserRepository.existsByEmail(event.getEmail())).willReturn(false);
        given(authUserRepository.save(any(AuthUser.class))).willAnswer(invocation -> {
            AuthUser user = invocation.getArgument(0);
            return user;
        });

        // when
        authService.registerUserFromEvent(event);

        // then
        verify(authUserRepository, times(1)).existsByEmail(event.getEmail());
        verify(authUserRepository, times(1)).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("이벤트로부터 사용자 등록 - 이미 존재하는 이메일")
    void registerUserFromEvent_DuplicateEmail() {
        // given
        UserRegisteredEvent event = new UserRegisteredEvent(
                1L,
                "existing@example.com",
                "홍길동",
                "010-1234-5678",
                "$2a$10$hashedPassword",
                LocalDateTime.now()
        );

        given(authUserRepository.existsByEmail(event.getEmail())).willReturn(true);

        // when
        authService.registerUserFromEvent(event);

        // then
        verify(authUserRepository, times(1)).existsByEmail(event.getEmail());
        verify(authUserRepository, never()).save(any(AuthUser.class));
    }
}

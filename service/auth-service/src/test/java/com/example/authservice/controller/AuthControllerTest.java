package com.example.authservice.controller;

import com.example.authservice.dto.request.LoginRequest;
import com.example.authservice.dto.request.RefreshTokenRequest;
import com.example.authservice.dto.response.TokenResponse;
import com.example.authservice.exception.*;
import com.example.authservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 성공")
    void login_Success() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        TokenResponse response = TokenResponse.of(
                "accessToken",
                "refreshToken",
                1800000L
        );

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 누락")
    void login_Fail_MissingEmail() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 누락")
    void login_Fail_MissingPassword() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 이메일 형식")
    void login_Fail_InvalidEmailFormat() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 인증 정보")
    void login_Fail_InvalidCredentials() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("로그인 실패 - 정지된 계정")
    void login_Fail_SuspendedAccount() throws Exception {
        // given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new AccountSuspendedException("정지된 계정입니다."));

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_Success() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validRefreshToken");

        TokenResponse response = TokenResponse.of(
                "newAccessToken",
                "newRefreshToken",
                1800000L
        );

        given(authService.refresh(any(RefreshTokenRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("newRefreshToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800));

        verify(authService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 리프레시 토큰 누락")
    void refresh_Fail_MissingRefreshToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refresh_Fail_InvalidRefreshToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalidToken");

        given(authService.refresh(any(RefreshTokenRequest.class)))
                .willThrow(new InvalidTokenException("유효하지 않은 토큰입니다."));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 만료된 리프레시 토큰")
    void refresh_Fail_ExpiredRefreshToken() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expiredToken");

        given(authService.refresh(any(RefreshTokenRequest.class)))
                .willThrow(new TokenExpiredException("만료된 토큰입니다."));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 사용자를 찾을 수 없음")
    void refresh_Fail_UserNotFound() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("validToken");

        given(authService.refresh(any(RefreshTokenRequest.class)))
                .willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(authService, times(1)).refresh(any(RefreshTokenRequest.class));
    }
}

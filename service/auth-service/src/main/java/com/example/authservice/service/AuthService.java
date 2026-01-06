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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() == AuthUser.UserStatus.SUSPENDED) {
            throw new AccountSuspendedException("정지된 계정입니다.");
        }

        if (user.getStatus() == AuthUser.UserStatus.INACTIVE) {
            throw new AccountSuspendedException("비활성화된 계정입니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        log.info("사용자 로그인 성공. userId: {}", user.getUserId());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessToken().getExpiration()
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        Claims claims = jwtTokenProvider.validateRefreshToken(request.getRefreshToken());
        Long userId = Long.parseLong(claims.getSubject());

        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != AuthUser.UserStatus.ACTIVE) {
            throw new AccountSuspendedException("계정이 활성 상태가 아닙니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);

        log.info("토큰 갱신 성공. userId: {}", userId);

        return TokenResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getAccessToken().getExpiration()
        );
    }

    @Transactional
    public void registerUserFromEvent(UserRegisteredEvent event) {
        // 이메일 중복 체크 (이미 등록된 사용자는 skip)
        if (authUserRepository.existsByEmail(event.getEmail())) {
            log.info("이미 등록된 사용자입니다. email: {}", event.getEmail());
            return;
        }

        // AuthUser 생성 (비밀번호는 이미 해시된 상태)
        AuthUser authUser = AuthUser.builder()
                .email(event.getEmail())
                .password(event.getHashedPassword())
                .status(AuthUser.UserStatus.ACTIVE)
                .build();

        authUserRepository.save(authUser);
        log.info("UserRegisteredEvent로부터 AuthUser 생성 완료. email: {}, userId: {}",
                event.getEmail(), authUser.getUserId());
    }
}

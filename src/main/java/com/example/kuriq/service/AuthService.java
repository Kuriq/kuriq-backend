package com.example.kuriq.service;

import com.example.kuriq.dto.user.request.LoginRequest;
import com.example.kuriq.dto.user.request.SignupRequest;
import com.example.kuriq.entity.user.LoginAttempt;
import com.example.kuriq.entity.user.RefreshToken;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.user.LoginAttemptRepository;
import com.example.kuriq.repository.user.RefreshTokenRepository;
import com.example.kuriq.repository.user.UserRepository;
import com.example.kuriq.security.JwtProvider;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public String signup(SignupRequest req) {
        if (userRepository.existsByEmailAndIsDeletedFalse(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .authProvider(User.AuthProvider.LOCAL)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        return user.getId();
    }

    // 로그인 후 Access/Refresh Token 발급
    public String[] loginAndGetTokens(LoginRequest req, String ip) {
        long failCount = loginAttemptRepository.countFailedAttempts(
                req.getEmail(),
                LocalDateTime.now().minusMinutes(30)
        );

        if (failCount >= 5) {
            throw new IllegalStateException("로그인 시도가 너무 많습니다. 30분 후 다시 시도해 주세요.");
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(req.getEmail())
                .orElseThrow(() -> {
                    loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), false, ip));
                    return new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), false, ip));
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptRepository.save(LoginAttempt.of(req.getEmail(), true, ip));

        String accessToken = jwtProvider.generateAccessToken(user.getId());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), refreshToken);

        return new String[]{accessToken, refreshToken};
    }

    // Refresh Token으로 토큰 재발급
    public String[] refresh(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndIsRevokedFalse(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다. 다시 로그인해 주세요."));

        refreshToken.revoke();

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String newAccessToken = jwtProvider.generateAccessToken(user.getId());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId());

        saveRefreshToken(user.getId(), newRefreshToken);

        return new String[]{newAccessToken, newRefreshToken};
    }

    // 로그아웃
    public void logout(String rawToken) {
        refreshTokenRepository
                .findByTokenHashAndIsRevokedFalse(sha256(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    // Refresh Token 원문을 해시로 바꿔 DB에 저장
    private void saveRefreshToken(String userId, String rawToken) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProvider.getRefreshTokenExpiryMs() / 1000);

        refreshTokenRepository.save(
                RefreshToken.create(userId, sha256(rawToken), expiresAt)
        );
    }

    // SHA-256 해시 변환
    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(input.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }
}
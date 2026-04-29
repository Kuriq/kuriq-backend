package com.example.kuriq.service;

import com.example.kuriq.dto.user.request.LoginRequest;
import com.example.kuriq.dto.user.request.SignupRequest;
import com.example.kuriq.entity.notification.NotificationSetting;
import com.example.kuriq.entity.user.LoginAttempt;
import com.example.kuriq.entity.user.PasswordResetToken;
import com.example.kuriq.entity.user.RefreshToken;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.user.LoginAttemptRepository;
import com.example.kuriq.repository.user.PasswordResetTokenRepository;
import com.example.kuriq.repository.user.RefreshTokenRepository;
import com.example.kuriq.repository.user.UserRepository;
import com.example.kuriq.security.JwtProvider;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;  // 비번 재설정 시 이메일 발송 필드

    // 회원가입
    public String signup(SignupRequest req) {

        User existing = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (existing != null) {
            if (!existing.getIsDeleted()) {
                // 소셜 계정으로 가입된 경우 별도 안내
                if (existing.getAuthProvider() != User.AuthProvider.LOCAL) {
                    throw new IllegalArgumentException(
                            "이미 " + existing.getAuthProvider() + " 계정으로 가입된 이메일입니다. 해당 계정으로 로그인해 주세요."
                    );
                }

                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            // soft delete된 계정이면 재활성화
            existing.reactivate(passwordEncoder.encode(req.getPassword()), req.getName());
            return existing.getId();
        }

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .name(req.getName())
                .authProvider(User.AuthProvider.LOCAL)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        // 유저가 생성되는 시점인 회원가입 때 알림 설정이 같이 만들어져야 함(1:1관계라서)
        // 즉, 회원가입할 때 notification_settings 테이블에 데이터가 자동으로 들어가야 함
        notificationSettingRepository.save(NotificationSetting.createDefault(user.getId()));
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

        // 소셜 전용 계정 체크(소셜 계정은 password가 null이라서 비번 체크 앞에 와야 함)
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException(
                    "이 이메일은 " + user.getAuthProvider() + " 로그인으로 가입되었습니다. "
                            + user.getAuthProvider() + " 로그인을 이용해 주세요."
            );
        }

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

    // 비밀번호 재설정 요청
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email).orElse(null);

        // 보안상 존재하지 않는 이메일도 성공 응답 반환 (이메일 존재 여부 노출 방지)
        if (user == null) return;

        // 소셜 로그인 전용 계정은 비밀번호 재설정 불가
        if (user.getAuthProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException(
                    "이 계정은 " + user.getAuthProvider() + " 로그인으로 가입되었습니다. "
                            + user.getAuthProvider() + "에서 비밀번호를 관리해 주세요."
            );
        }

        // 기존 미사용 토큰 무효화
        passwordResetTokenRepository.invalidatePreviousTokens(user.getId());

        // 새 토큰 생성
        String rawToken = java.util.UUID.randomUUID().toString();
        passwordResetTokenRepository.save(
                PasswordResetToken.create(user.getId(), sha256(rawToken)) // DB에 해시로 저장
        );

        // 이메일 발송
        emailService.sendPasswordResetEmail(email, rawToken);
    }

    // 비밀번호 재설정 확인
    public void confirmPasswordReset(String rawToken, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                // 사용자가 보내온 토큰을 해시 처리해서 DB에 저장된 해시값과 비교해 토큰 찾음
                .findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 토큰입니다.");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.changePassword(passwordEncoder.encode(newPassword));  // passwordEncoder로 암호화된 새 비밀번호를 받아 교체
        resetToken.markUsed();  // 재사용 방지
    }
}
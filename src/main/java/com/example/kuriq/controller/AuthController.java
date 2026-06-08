package com.example.kuriq.controller;

import com.example.kuriq.dto.user.request.PasswordResetConfirmRequest;
import com.example.kuriq.dto.user.request.PasswordResetRequest;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import com.example.kuriq.dto.user.response.AuthResponse;
import com.example.kuriq.dto.user.request.LoginRequest;
import com.example.kuriq.dto.user.request.SignupRequest;
import com.example.kuriq.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final int    COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일(초)

    // 회원가입
    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest req) {
        String userId = authService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("userId", userId, "message", "회원가입이 완료되었습니다"));
    }

    // 로그인
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        String[] tokens = authService.loginAndGetTokens(req, extractClientIp(httpReq));
        setRefreshCookie(httpRes, tokens[1]);
        return ResponseEntity.ok(AuthResponse.of(tokens[0]));
    }

    // 로그아웃
    @Operation(summary = "로그아웃",
            description = "refreshToken은 HttpOnly 쿠키로 자동 전송됩니다. 별도 입력 불필요.")

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        String token = extractRefreshCookie(httpReq); // 쿠키에서 refreshToken 추출
        if (token != null) authService.logout(token); // 토큰이 있으면 DB에서 무효화(revoke)
        clearRefreshCookie(httpRes); // 클라이언트 쿠키 삭제
        return ResponseEntity.noContent().build(); // 204 No Content 반환
    }

    /** 토큰 갱신 */

    // Swagger 문서에 refreshToken 쿠키 파라미터 표시
    // HttpOnly라 UI에서 직접 입력은 불가, 로그인 후 브라우저 쿠키 자동 전송
    @Operation(summary = "토큰 갱신",
            description = "refreshToken은 HttpOnly 쿠키로 자동 전송됩니다. 별도 입력 불필요.")

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        String token = extractRefreshCookie(httpReq);
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 쿠키 없으면 401 반환
        String[] newTokens = authService.refresh(token);  // 토큰 검증 후 새 토큰 쌍 발급 (Token Rotation)
        setRefreshCookie(httpRes, newTokens[1]);  // 새 refreshToken을 쿠키로 교체
        return ResponseEntity.ok(AuthResponse.of(newTokens[0]));  // 새 accessToken만 바디로 반환
    }

    // Refresh Token을 HttpOnly 쿠키로 설정 (JS 접근 불가 → XSS 방어)
    private void setRefreshCookie(HttpServletResponse res, String token) {
        Cookie c = new Cookie(REFRESH_COOKIE, token);
        c.setHttpOnly(true);
        c.setSecure(true);     // HTTPS only
        c.setPath("/api/v1/auth");
        c.setMaxAge(COOKIE_MAX_AGE);
        res.addCookie(c);
    }

    // 쿠키 삭제 (Max-Age=0)
    private void clearRefreshCookie(HttpServletResponse res) {
        Cookie c = new Cookie(REFRESH_COOKIE, "");
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/api/v1/auth");
        c.setMaxAge(0);
        res.addCookie(c);
    }

    // 쿠키에서 Refresh Token 꺼내기
    private String extractRefreshCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    // Nginx 프록시 거칠 때 실제 IP는 X-Forwarded-For 헤더에 있음
    private String extractClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
    }

    // 비밀번호 재설정 요청(이메일 입력 → 토큰 생성 및 이메일 발송)
    @Operation(summary = "비밀번호 재설정 요청")
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest req) {
        authService.requestPasswordReset(req.getEmail());  // 이메일로 재설정 토큰 생성 및 발송 처리
        return ResponseEntity.noContent().build();  // 성공 시 204 No Content 반환 (응답 바디 없음)
    }

    // 비밀번호 재설정 확인(토큰 검증 → 새 비밀번호로 변경)
    @Operation(summary = "비밀번호 재설정 확인")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest req) {
        // 토큰 검증 후 비밀번호 변경 처리
        authService.confirmPasswordReset(req.getToken(), req.getNewPassword());
        // 성공 시 204 No Content 반환
        return ResponseEntity.noContent().build();
    }
}
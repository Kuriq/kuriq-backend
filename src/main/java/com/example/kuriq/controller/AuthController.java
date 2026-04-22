package com.example.kuriq.controller;

import com.example.kuriq.dto.user.AuthResponse;
import com.example.kuriq.dto.user.LoginRequest;
import com.example.kuriq.dto.user.SignupRequest;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final int    COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일(초)

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @Valid @RequestBody SignupRequest req) {
        String userId = authService.signup(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("userId", userId, "message", "회원가입이 완료되었습니다"));
    }

    // 로그인
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
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        String token = extractRefreshCookie(httpReq);
        if (token != null) authService.logout(token);
        clearRefreshCookie(httpRes);
        return ResponseEntity.noContent().build();
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) {
        String token = extractRefreshCookie(httpReq);
        if (token == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String[] newTokens = authService.refresh(token);
        setRefreshCookie(httpRes, newTokens[1]);
        return ResponseEntity.ok(AuthResponse.of(newTokens[0]));
    }

    // 쿠키 헬퍼

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
}
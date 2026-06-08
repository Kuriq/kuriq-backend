package com.example.kuriq.dto.user.response;

import lombok.Builder;
import lombok.Getter;

/**
 * =====================================================
 * 로그인 / 토큰 갱신 응답 DTO
 * =====================================================
 *
 * POST /api/v1/auth/login 응답:
 * {
 *   "accessToken": "eyJhbGci...",
 *   "tokenType": "Bearer",
 *   "expiresIn": 3600
 * }
 *
 * Refresh Token은 바디에 포함하지 않음
 *    HttpOnly 쿠키로 별도 전달 → JS로 읽을 수 없어 보안상 안전.
 */

@Getter
@Builder
public class AuthResponse {

    // JWT Access Token: 로그인 인증용 토큰(JWT)
    private String accessToken;

    public static AuthResponse of(String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .build();

    }
}

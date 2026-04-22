package com.example.kuriq.dto.user;

import jakarta.validation.constraints.*;
import lombok.Getter;

// ──────────────────────────────────────────
// 로그인 요청
// ──────────────────────────────────────────

/**
 * POST /api/v1/auth/login
 *
 * 요청 바디:
 * { "email": "user@example.com", "password": "password123" }
 */

@Getter
public class LoginRequest {
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일을 입력해 주세요")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요")
    private String password;
}

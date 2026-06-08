package com.example.kuriq.dto.user.request;

import com.example.kuriq.entity.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;  // email, notblank 등등 어노테이션들 임포트
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * =====================================================
 * 인증 요청 DTO 모음
 * =====================================================
 * POST /api/v1/auth/signup  → SignupRequest
 * POST /api/v1/auth/login   → LoginRequest
 * POST /api/v1/auth/password-reset/request  → PasswordResetRequestDto
 * POST /api/v1/auth/password-reset/confirm  → PasswordResetConfirmDto
 */

// ──────────────────────────────────────────
// 회원가입 요청
// ──────────────────────────────────────────

/**
 * POST /api/v1/auth/signup
 *
 * 요청 바디 예시:
 * {
 *   "email": "user@example.com",
 *   "password": "password123",
 *   "name": "홍길동",
 *   "ageGroup": "THIRTIES"   ← 선택 사항
 * }
 */

@NoArgsConstructor
@Getter
public class SignupRequest {

    @Schema(example = "test@test.com")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일을 입력해 주세요")
    private String email;

    @Schema(example = "김현정")
    @NotBlank(message = "이름을 입력해 주세요")
    @Size(max = 20, message = "이름은 20자 이하로 입력해 주세요")
    private String name;

    @Schema(example = "test1234")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
            message = "비밀번호는 영문과 숫자를 포함해야 합니다."
    )
    private String password;

    private User.AgeGroup ageGroup;
}

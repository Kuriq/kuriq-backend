package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;

/**
 * POST /api/v1/auth/login
 *
 * 요청 바디:
 * { "email": "user@example.com", "password": "password123" }
 */

@Getter
public class LoginRequest {

    @Schema(example = "hyunjung26@skuniv.ac.kr")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @NotBlank(message = "이메일을 입력해 주세요")
    private String email;

    @Schema(example = "test1234")
    @NotBlank(message = "비밀번호를 입력해 주세요")
    private String password;
}

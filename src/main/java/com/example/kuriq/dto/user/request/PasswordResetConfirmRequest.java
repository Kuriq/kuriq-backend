package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 비밀번호 재설정 시 프론트엔드에서 전달된 토큰과 새 비밀번호를 서버로 전달하는 요청 DTO
 */

@Getter
@Schema(description = "비밀번호 재설정 확인 요청")
public class PasswordResetConfirmRequest {

    // 이메일로 받은 링크에 토큰 포함됨
    @Schema(description = "이메일로 받은 재설정 토큰", example = "abc123token")
    @NotBlank(message = "토큰을 입력해 주세요")
    private String token;

    @Schema(description = "새 비밀번호 (8자 이상)", example = "newPassword123!")
    @NotBlank(message = "새 비밀번호를 입력해 주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}

package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 비밀번호 재설정을 위해 사용자의 이메일을 입력받는 요청 DTO
 */

@Getter
@Schema(description = "비밀번호 재설정 요청")
public class PasswordResetRequest {

    @Schema(description = "가입한 이메일", example = "hyunjung26@skuniv.ac.kr")
    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;
}

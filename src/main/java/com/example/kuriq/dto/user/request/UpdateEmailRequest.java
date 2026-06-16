package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "이메일 주소 수정 요청")
public class UpdateEmailRequest {

    @NotBlank(message = "이메일을 입력해 주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;
}

package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "내 프로필 수정 요청")
public class UserProfileUpdateRequest {

    @NotBlank(message = "닉네임을 입력해 주세요")
    @Size(max = 20, message = "닉네임은 최대 20자까지 입력할 수 있습니다")
    private String name;

    @NotBlank(message = "프로필 아이콘을 선택해 주세요")
    @Size(max = 8, message = "프로필 아이콘 형식이 올바르지 않습니다")
    private String profileIcon;

    @NotBlank(message = "프로필 색상을 선택해 주세요")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "프로필 색상 형식이 올바르지 않습니다")
    private String profileColor;
}

package com.example.kuriq.dto.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** 회원 탈퇴 요청 시, 사용자 본인 확인(비밀번호 검증)을 위해 보내는 데이터 */

@Getter
@Schema(description = "회원 탈퇴 요청")
public class DeleteAccountRequest {
    @Schema(description = "현재 비밀번호 (소셜 계정은 null)", example = "myPassword123!")
    private String password;  // 일반 회원은 비밀번호 다시 입력, 소셜 로그인은 비밀번호 없으니까 null
}

package com.example.kuriq.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SocialCallbackRequest {

    @NotBlank(message = "provider는 필수입니다 (kakao, google, naver)")
    private String provider;  // 소셜 플랫폼 구분 (kakao / google / naver)

    @NotBlank(message = "인증 code는 필수입니다")
    private String code;      // 소셜 플랫폼에서 발급한 인증 코드
}

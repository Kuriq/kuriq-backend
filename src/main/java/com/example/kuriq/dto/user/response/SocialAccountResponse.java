package com.example.kuriq.dto.user.response;

import com.example.kuriq.entity.user.SocialAccount;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** 소셜 계정 정보(provider, email, linkedAt)를 클라이언트에 반환하는 응답 DTO */

@Getter
@Builder
public class SocialAccountResponse {
    private String id;
    private SocialAccount.Provider provider;
    private String email;
    private LocalDateTime linkedAt;

    // socialId는 소셜 플랫폼 내부 식별자라 프론트에서 쓸 일이 없고, 노출할 필요도 없어서 제외함
    public static SocialAccountResponse from(SocialAccount sa) {
        return SocialAccountResponse.builder()
                .id(sa.getId())
                .provider(sa.getProvider())
                .email(sa.getEmail())
                .linkedAt(sa.getLinkedAt())
                .build();
    }
}

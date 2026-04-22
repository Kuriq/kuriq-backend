package com.example.kuriq.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * 어떤 사용자가 어떤 소셜 계정을 연결했는지 저장하는 테이블
 * ERD: social_accounts 테이블
 *
 * 한 사용자가 여러 소셜 계정을 연동할 수 있다 (users 1:N social_accounts).
 * (provider, social_id) 복합 유니크 — 같은 프로바이더에서 같은 소셜 ID는 한 번만 등록.
 *
 * 예) userId=abc 가 구글, 카카오 동시 연동 가능
 */
@Entity
@Table(
        name = "social_accounts",
        indexes = {
                // 같은 소셜 계정이 중복 저장되면 안돼서 인덱스 지정
                @Index(name = "uk_provider_social_id", columnList = "provider, socialId", unique = true),
                // userId 기준으로 빠르게 조회할 수 있게 인덱스 만들기
                @Index(name = "idx_social_user", columnList = "userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider;

    /**
     * 프로바이더가 부여한 고유 사용자 식별자
     */
    @Column(nullable = false, length = 255)
    private String socialId;

    /**
     * 프로바이더에서 제공한 이메일 (nullable)
     */
    @Column(length = 255)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    private void prePersist() {
        linkedAt = LocalDateTime.now();
    }

    public enum Provider {GOOGLE, NAVER, KAKAO}

    public static SocialAccount create(String userId, Provider provider,
                                       String socialId, String email) {
        SocialAccount sa = new SocialAccount();
        sa.userId = userId;
        sa.provider = provider;
        sa.socialId = socialId;
        sa.email = email;
        return sa;
    }
}

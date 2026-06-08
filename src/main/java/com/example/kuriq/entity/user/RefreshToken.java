package com.example.kuriq.entity.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = { // 보안을 위해 index 필요
                @Index(name = "uk_token_hash", columnList = "tokenHash", unique = true)
                // tokenHash는 중복되면 안됨 -> unique = true
        }
)

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String userId;  // 어떤 유저의 토큰인지, 1:N 관계

    /** Refresh Token 원문의 SHA-256 해시 */
    @Column(nullable = false, length =255)
    private String tokenHash;  // 실제 토큰 x, SHA-256 해시만 저장 (보안 핵심)

    @Column(nullable = false)   // 만료 시간 없는 토큰은 위험
    private LocalDateTime expiresAt;    // 만료 시간, JWT 만료랑 맞춰야 함

    /** 로그아웃 또는 탈취 감지 시 true */
    @Column(nullable = false)
    private Boolean isRevoked = false;  // 로그아웃됐나 => true면 사용금지

    @Column(nullable = false, updatable = false)  // update 시점에 변경되지 않게
    private LocalDateTime createdAt;	// 언제 발급됐는지, 로그 추적용

    @PrePersist
    private void prePersist() { // DB에 INSERT 되기 직전에 자동 실행되는 메서드
        createdAt = LocalDateTime.now();   // 엔티티 내부에서 자동으로 세팅되게 (매번 Service에서 코드 작성할 필요 없음)
    }

    // 객체 생성용 팩토리 메서드
    public static RefreshToken create(String userId, String tokenHash, LocalDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.userId = userId;
        rt.tokenHash = tokenHash;
        rt.expiresAt = expiresAt;
        return rt;
    }

    public void revoke() {  // 로그아웃하면 호출
        this.isRevoked = true;  // true로 변경돼서 토큰 사용금지됨
    }

    // 현재 시간이 expiresAt 이후이면 만료된 것으로 판단
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

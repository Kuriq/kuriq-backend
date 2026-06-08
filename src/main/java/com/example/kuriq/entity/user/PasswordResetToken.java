package com.example.kuriq.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * ERD: password_reset_tokens 테이블
 *
 * 비밀번호 재설정 기능에서 사용하는 토큰 엔티티
 * - 사용자가 "비밀번호 찾기" 요청 시 이메일로 링크 발송
 * - 해당 링크에 포함된 토큰을 DB에 저장
 *
 * 정책:
 * - 토큰 유효시간: 1시간
 * - 1회 사용 후 used = true 처리
 * - 실제 토큰은 저장하지 않고 SHA-256 해시값만 저장 (보안)
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                // 토큰 해시는 유일해야 함 (중복 방지)
                @Index(name = "uk_reset_token", columnList = "tokenHash", unique = true),
                // 특정 사용자의 토큰 조회 성능 향상
                @Index(name = "idx_reset_user", columnList = "userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @UuidGenerator  // 토큰 ID (UUID 자동 생성)
    @Column(length = 36)
    private String id;

    // 토큰을 발급받은 사용자 ID
    @Column(nullable = false, length = 36)
    private String userId;

    /** 토큰 SHA-256 해시값 */
    @Column(nullable = false, length = 255)
    private String tokenHash;

    // 토큰 만료 시간 (생성 시점 + 1시간)
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 사용 여부 (true면 이미 사용된 토큰 → 재사용 불가)
    @Column(nullable = false)
    private Boolean used = false;

    // 생성 시간 (최초 생성 시 자동 저장, 수정 불가)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 엔티티가 처음 DB에 저장되기 직전에 실행
    // 생성 시간을 자동으로 현재 시간으로 설정
    @PrePersist
    private void prePersist() { createdAt = LocalDateTime.now(); }

    // 토큰 생성 메서드
    // userId, tokenHash를 받아서 객체 생성
    // 만료 시간은 현재 기준 +1시간으로 설정
    public static PasswordResetToken create(String userId, String tokenHash) {
        PasswordResetToken t = new PasswordResetToken();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.expiresAt = LocalDateTime.now().plusHours(1);
        return t;
    }

    // 토큰 유효성 검사
    // 아직 사용되지 않았고 현재 시간이 만료 시간 이전이면 유효
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    // 토큰 사용 처리
    // 사용 완료 시 true로 변경해서 재사용 방지
    public void markUsed() { this.used = true; }
}


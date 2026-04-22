package com.example.kuriq.entity.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ERD: login_attempts 테이블
 *
 * 로그인 시도 이력을 기록한다.
 * 5분 내 5회 실패 시 → 해당 이메일 30분 잠금.
 * 잠금 판단은 UserService에서 이 테이블을 조회해 계산한다.
 * 즉, 실패횟수 기록이 필요함.
 * userId 필요없음 -> 로그인 실패는 유저 존재 여부와 상관없이 기록해야 함
 */
@Entity
@Table(
        name = "login_attempts",
        indexes = { // 이메일과 시간 기준으로 검색 빠르게
                @Index(name = "idx_login_email_time", columnList = "email, attemptedAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt { // 로그인 시도할 때마다 기록 남기는 DB 테이블
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 PK
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;   // 어떤 계정으로 로그인 시도했는지

    @Column(nullable = false)
    private LocalDateTime attemptedAt;  // 언제 시도했느닞

    //
    @Column(nullable = false)
    private boolean success;   // true == 로그인 성공, false == 로그인 실패

    /** IPv6 지원 — 최대 45자 */
    @Column(length = 45)
    private String ipAddress;   // 어디서 로그인했는지

    @PrePersist // DB 저장 직전에 자동 실행
    private void prePersist() { attemptedAt = LocalDateTime.now(); }

    // of() 메서드 -> 객체 생성용 메서드
    public static LoginAttempt of(String email, boolean success, String ipAddress) {
        LoginAttempt la = new LoginAttempt();
        la.email = email;
        la.success = success;
        la.ipAddress = ipAddress;
        return la;
    }

    /** 실제 흐름:
     * 로그인 시도:
     * LoginAttempt.of(email, false, ip)
     * → DB 저장
     * 로그인 실패 체크:
     * 최근 5분 실패 횟수 조회
     * 조건 만족하면:
     * 계정 잠금
     * 로그인 성공 시:
     * LoginAttempt.of(email, true, ip)
     */

}

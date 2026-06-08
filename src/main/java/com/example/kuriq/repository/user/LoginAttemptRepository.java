package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 최근 로그인 실패 횟수 세는 저장소: 특정 이메일이 최근 몇 분 동안 로그인에 몇 번 실패했나
 *
 * 최근 30분 내 실패 횟수가 5회 이상이면 AccountLockedException 발생.
 * IP 기반이 아닌 이메일 기반 잠금 — 공유 IP(공공 WiFi 등)에서 다른 사용자 영향 최소화.
 */


public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    /**
     * 특정 이메일의 최근 N분 내 실패 횟수 조회.
     * AuthService에서 since = LocalDateTime.now().minusMinutes(30)으로 호출.
     */

    @Query("""
            SELECT COUNT(la) FROM LoginAttempt la
            WHERE la.email = :email
            AND la.success = false
            AND la.attemptedAt >= :since
            """)
    long countFailedAttempts(@Param("email") String email,
                             @Param("since") LocalDateTime since);
}

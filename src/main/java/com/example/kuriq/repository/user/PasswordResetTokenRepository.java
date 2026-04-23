package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 비밀번호 재설정 토큰 저장소 (Repository)
 *
 * 역할:
 * - 토큰 조회 (tokenHash 기준)
 * - 기존 토큰 무효화 처리
 *
 * 특징:
 * - 실제 토큰이 아닌 SHA-256 해시값(tokenHash)으로 조회
 * - 새로운 토큰 발급 시 기존 미사용 토큰을 모두 used = true 처리
 */

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    // 토큰 해시값으로 토큰 조회
    // @param tokenHash SHA-256으로 해싱된 토큰 값
    // @return 해당 토큰 (없으면 Optional.empty)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // 특정 사용자(userId)의 기존 미사용 토큰들을 모두 무효화
    // used = false → true로 변경
    // 새로운 토큰 발급 전에 호출하여 기존 토큰 재사용 방지
    // @param userId 사용자 ID
    @Modifying  // UPDATE/DELETE 쿼리임을 명시
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.userId = :userId AND t.used = false")
    void invalidatePreviousTokens(@Param("userId") String userId);
}

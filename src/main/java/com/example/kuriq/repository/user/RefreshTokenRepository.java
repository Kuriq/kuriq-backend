package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Refresh Token 저장소.
 *
 * 토큰 원문 대신 SHA-256 해시(tokenHash)로 조회한다.
 * Token Rotation: 사용된 토큰은 즉시 revoke, 신규 토큰 발급.
 * 다중 디바이스: userId 1 : N refresh_tokens.
 */


public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

    /** 해당 사용자의 모든 토큰 무효화(전체 기기 로그아웃) */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.userId = :userId")
    int revokeAllByUserId(@Param("userId") String userId);
}

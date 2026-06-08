package com.example.kuriq.security;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Access Token 생성, 검증, payload 조회를 담당
 *
 * Access Token: userId, email 등의 인증 정보를 담음
 * Refresh Token: 재발급 용도 식별 정보를 담음
 *
 * Access Token payload:
 *   - sub: userId (UUID)
 *   - email: 사용자 이메일
 *   - iat: 발급 시각
 *   - exp: 만료 시각
 */

@Slf4j  // 로그 객체 log를 자동으로 만들어줌
@Component  // Spring이 이 클래스를 객체로 만들어 관리하게 함
public class JwtProvider {
    private final SecretKey key;  // 토큰 만들 때, 검증할 때 사용할 비밀키
    private final long accessTokenExpiry;   // Access Token 유효시간(단위: ms)
    private final long refreshTokenExpiry;  // Refresh Token 유효시간(단위: ms)

    // application 설정값(jwt.secret, 만료시간)을 주입받아
    // JWT 서명에 사용할 SecretKey와 토큰 만료시간을 초기화한다.
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    // 발급
    // 로그인 성공 시 access token 생성에 사용됨
    public String generateAccessToken(String userId) {
        Date now = new Date();  // 현재 시간 저장
        return Jwts.builder()   // jwt를 만들기 시작함(여기서 claims 만들어짐)
                .subject(userId)    // JWT의 sub값으로 userId를 넣음
                .claim("type", "access")    // 토큰 타입
                .issuedAt(now)  // 발급 시간
                .expiration(new Date(now.getTime() + accessTokenExpiry))  // 만료 시간
                .signWith(key)  // 비밀키로 서명
                .compact(); // 최종적으로 JWT 문자열로 만들어 반환
    }

    // Refresh Token 발급
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiry))
                .signWith(key)
                .compact();
    }

    // 토큰 검증/파싱
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {   // 토큰 만료나 JWT 관련 오류가 있을 때 발생
            log.debug("JWT invalid: {}", e.getMessage());
            return false;
        }
    }
    public String getUserId(String token) { // 토큰 안의 sub값을 꺼내는 메서드
        return getClaims(token).getSubject();   // userId 반환
    }

    // refresh token 만료시간 반환
    public long getRefreshTokenExpiryMs() { return refreshTokenExpiry; }

    // 내부 claims 파싱
    private Claims getClaims(String token) {    // 토큰을 검증하면서 payload를 가져오는 메서드
        return Jwts.parser()    // JWT 파서 생성
                .verifyWith(key)    // 이 키로 서명 검증한다는 의미
                .build()    // 파서 완성
                .parseSignedClaims(token)  // 토큰을 파싱하면서 서명 검증
                .getPayload();  // payload 부분만(여기서는 claims) 부분만 꺼냄
    }
}



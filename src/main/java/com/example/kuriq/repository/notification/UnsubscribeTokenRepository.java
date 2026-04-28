package com.example.kuriq.repository.notification;

import com.example.kuriq.entity.notification.UnsubscribeToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 알림 수신 거부 토큰 저장소
// token(PK)으로 조회해서 userId와 channel을 꺼낸다
public interface UnsubscribeTokenRepository extends JpaRepository<UnsubscribeToken, String> {

    // token 값으로 DB 조회
    Optional<UnsubscribeToken> findByToken(String token);
}

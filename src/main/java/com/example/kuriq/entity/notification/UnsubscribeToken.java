package com.example.kuriq.entity.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 알림 수신을 거부할 때 사용하는 일회용 링크 토큰을 저장하는 테이블
 * 이메일/카카오 알림에서 “수신 거부하기” 클릭했을 때 사용하는 토큰 관리
 * userId가 아닌 토큰 방식의 장점: 로그인 없이도 수신 거부 가능
 */

@Entity
@Table(name = "unsubscribe_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UnsubscribeToken {

    // 토큰
    @Id
    @Column(length = 255)
    private String token;

    // 어떤 사용자의 수신 거부인지
    @Column(nullable = false, length = 36)
    private String userId;

    // 어떤 알림을 끄는지 (EMAIL or KAKAO)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    // 토큰 생성 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // DB 저장 직전에 자동 실행(실수 방지)
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum Channel { EMAIL, KAKAO }

    // 엔티티 생성용 팩토리 메서드(new 대신 안전하게 생성)
    public static UnsubscribeToken create(String token, String userId, Channel channel) {
        UnsubscribeToken t = new UnsubscribeToken();
        t.token = token;
        t.userId = userId;
        t.channel = channel;
        return t;
    }
}

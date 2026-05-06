package com.example.kuriq.service;

import com.example.kuriq.entity.notification.UnsubscribeToken;
import com.example.kuriq.repository.notification.UnsubscribeTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 수신 거부 토큰 생성 및 저장 서비스 (EmailService와 분리하여 트랜잭션 독립 적용)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UnsubscribeTokenService {

    private final UnsubscribeTokenRepository unsubscribeTokenRepository;

    private static final String BASE_URL = "https://kuriq.com";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createUnsubscribeLink(String userId, UnsubscribeToken.Channel channel) {
        String token = UUID.randomUUID().toString();
        UnsubscribeToken saved = unsubscribeTokenRepository.save(
                UnsubscribeToken.create(token, userId, channel)
        );
        return BASE_URL + "/api/v1/notifications/unsubscribe?token=" + token;
    }
}

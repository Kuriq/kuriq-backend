package com.example.kuriq.service;

import com.example.kuriq.dto.notification.request.NotificationUpdateRequest;
import com.example.kuriq.dto.notification.response.NotificationResponse;
import com.example.kuriq.dto.user.response.SocialAccountResponse;
import com.example.kuriq.entity.notification.NotificationSetting;
import com.example.kuriq.entity.notification.UnsubscribeToken;
import com.example.kuriq.entity.user.SocialAccount;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.notification.UnsubscribeTokenRepository;
import com.example.kuriq.repository.user.RefreshTokenRepository;
import com.example.kuriq.repository.user.SocialAccountRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UnsubscribeTokenRepository unsubscribeTokenRepository;

    // 프로필 조회
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 소셜 계정 목록 조회
    public List<SocialAccountResponse> getSocialAccounts(String userId) {
        return socialAccountRepository.findByUserId(userId).stream()
                .map(SocialAccountResponse::from)
                .collect(Collectors.toList());
    }

    // 소셜 계정 연동 해제
    @Transactional
    public void unlinkSocialAccount(String userId, String providerStr) {
        SocialAccount.Provider provider =
                SocialAccount.Provider.valueOf(providerStr.toUpperCase());
        socialAccountRepository.findByUserId(userId).stream()
                .filter(sa -> sa.getProvider() == provider)
                .findFirst()
                .ifPresent(socialAccountRepository::delete);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteAccount(String userId, String password) {
        User user = getUser(userId);

        if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("비밀번호를 입력해 주세요");
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 올바르지 않습니다");
            }
        }
        user.softDelete();
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // 알림 설정 조회
    public NotificationResponse getNotificationSettings(String userId) {
        NotificationSetting ns = notificationSettingRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));
        return NotificationResponse.from(ns);
    }

    // 알림 설정 수정
    @Transactional
    public NotificationResponse updateNotificationSettings(String userId,
                                                           NotificationUpdateRequest req) {
        NotificationSetting ns = notificationSettingRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));
        ns.update(
                req.getEmailEnabled(),
                req.getKakaoEnabled(),
                req.getLearningDay(),
                req.getLearningTime(),
                req.getWeeklyStartAlert(),
                req.getIncompleteReminder(),
                req.getInactivityAlert(),
                req.getCompletionAlert()
        );
        return NotificationResponse.from(ns);
    }

    // 알림 수신 거부 (이메일 링크의 token으로 채널 비활성화)
    // 수신 거부 링크의 토큰으로 사용자와 채널을 찾아 해당 알림을 끄고, 토큰을 1회용으로 처리하는 서비스 로직
    @Transactional
    public void unsubscribeNotification(String token) {

        // token으로 어떤 사용자인지, 어떤 채널인지 확인
        UnsubscribeToken unsubscribeToken = unsubscribeTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 수신 거부 토큰입니다"));

        // 해당 사용자의 알림 설정 조회(사용자 알림 설정 엔티티 가져옴)
        NotificationSetting ns = notificationSettingRepository.findById(unsubscribeToken.getUserId())
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));

        // 해당 채널만 off
        if (unsubscribeToken.getChannel() == UnsubscribeToken.Channel.EMAIL) {
            ns.update(false, ns.getKakaoEnabled(),
                    ns.getLearningDay(), ns.getLearningTime(),
                    ns.getWeeklyStartAlert(), ns.getIncompleteReminder(),
                    ns.getInactivityAlert(), ns.getCompletionAlert());
        } else {
            ns.update(ns.getEmailEnabled(), false,
                    ns.getLearningDay(), ns.getLearningTime(),
                    ns.getWeeklyStartAlert(), ns.getIncompleteReminder(),
                    ns.getInactivityAlert(), ns.getCompletionAlert());
        }

        // 사용한 토큰 삭제 (1회용)
        unsubscribeTokenRepository.delete(unsubscribeToken);
    }
}
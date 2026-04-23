package com.example.kuriq.service;

import com.example.kuriq.entity.user.SocialAccount;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.user.SocialAccountRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    // 프로필 조회
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    // 소셜 계정
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
    public void deleteAccount(String userId) {
        getUser(userId).softDelete();
    }
}
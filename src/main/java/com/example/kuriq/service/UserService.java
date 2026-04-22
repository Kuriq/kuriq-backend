package com.example.kuriq.service;

import com.example.kuriq.entity.user.SocialAccount;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.SocialAccountRepository;
import com.example.kuriq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    @Transactional
    public void updateProfile(String userId, String name, String ageGroupStr) {
        User user = getUser(userId);
        // TODO: User.updateProfile() 구현 후 연결
    }

    public List<Map<String, String>> getSocialAccounts(String userId) {
        return socialAccountRepository.findByUserId(userId).stream()
                .map(sa -> Map.of(
                        "provider", sa.getProvider().name(),
                        "email",    sa.getEmail() != null ? sa.getEmail() : "",
                        "linkedAt", sa.getLinkedAt().toString()))
                .collect(Collectors.toList());
    }

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
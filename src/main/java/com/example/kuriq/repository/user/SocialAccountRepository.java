package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 소셜 계정 저장소.
 * (provider, socialId) 복합 유니크로 중복 연동 방지.
 */


public interface SocialAccountRepository extends JpaRepository<SocialAccount, String> {
    Optional<SocialAccount> findByProviderAndSocialId(
            SocialAccount.Provider provider, String socialId);  // provider + socialId로 유저 식별
    List<SocialAccount> findByUserId(String userId);  // 한 유저가 연결한 소셜 계정 목록 가져오기
    void deleteByUserId(String userId);  // 회원 탈퇴 시 소셜 계정 연동 정보 전체 삭제
}

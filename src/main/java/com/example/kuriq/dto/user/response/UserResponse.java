package com.example.kuriq.dto.user.response;

import com.example.kuriq.entity.user.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder    // response 객체는 우리가 직접 만들어야해서 builder 씀
public class UserResponse {
    private String id;
    private String email;
    private String name;
    private String profileIcon;
    private String profileColor;
    private User.AgeGroup ageGroup;
    private LocalDateTime createdAt;
    private String authProvider;  // 소셜/일반 계정 구분 (LOCAL | GOOGLE | NAVER | KAKAO)

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileIcon(user.getProfileIcon())
                .profileColor(user.getProfileColor())
                .ageGroup(user.getAgeGroup())
                .createdAt(user.getCreatedAt())
                .authProvider(user.getAuthProvider().name())  // 회원 탈퇴 시 소셜 계정 여부 판단용
                .build();
    }

}

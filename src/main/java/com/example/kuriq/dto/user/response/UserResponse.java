package com.example.kuriq.dto.user.response;

import com.example.kuriq.entity.user.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder    // response 객체는 우리가 직접 만들어야해서 builder 씀
public class UserResponse {
    private String id;
    private String email;
    private String name;
    private User.AgeGroup ageGroup;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                // .ageGroup(u.getAgeGroup())
                .build();
    }

}

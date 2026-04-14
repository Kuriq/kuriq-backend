package com.example.kuriq.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserSignupRequest {
    private String email;
    private String name;
    private String password;

    // 이메일, 이름, 비밀번호를 저장하는 생성자 추가
    public UserSignupRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }
}

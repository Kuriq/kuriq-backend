package com.example.kuriq.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder    // response 객체는 우리가 직접 만들어야해서 builder 씀
public class UserResponse {
    private Long id;
    private String email;
    private String name;

}

package com.example.kuriq.controller;

import com.example.kuriq.dto.user.UserResponse;
import com.example.kuriq.dto.user.UserSignupRequest;
import com.example.kuriq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController// RestAPI용 컨트롤러
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;  // 회원가입 처리해주는 service 가져다쓰기

    @PostMapping("/signup") // "/api/users/signup으로 POST 요청 오면 이 함수 실행"
    public ResponseEntity<UserResponse> signup(
            @RequestBody UserSignupRequest request  // 이 부분 아직 공부 안함
    ) {
        return ResponseEntity.ok(userService.signup(request));
    }
}

package com.example.kuriq.service;

import com.example.kuriq.dto.user.UserResponse;
import com.example.kuriq.dto.user.UserSignupRequest;
import com.example.kuriq.entity.User;
import com.example.kuriq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor    // @RequiredArgsConstructor는 초기화 되지않은 final 필드나, @NonNull 이 붙은 필드에 대해 생성자를 생성해 줌
public class UserService {
    private final UserRepository userRepository;
    // 회원가입 요청 데이터를 request라는 이름으로 받아서 -> 회원가입 처리 -> UserResponse 결과를 돌려주는 함수
    public UserResponse signup(UserSignupRequest request) {
        // 1. DTO를 엔티티로 변환
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .build();

        // 2. repository로 엔티티를 DB에 저장
        userRepository.save(user);

        // 3. Entity → DTO 변환 (응답)
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}

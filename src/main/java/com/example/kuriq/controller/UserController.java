package com.example.kuriq.controller;


import com.example.kuriq.dto.notification.request.NotificationUpdateRequest;
import com.example.kuriq.dto.notification.response.NotificationResponse;
import com.example.kuriq.dto.user.request.DeleteAccountRequest;
import com.example.kuriq.dto.user.response.SocialAccountResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.example.kuriq.dto.user.response.UserResponse;
import com.example.kuriq.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(userId)));
    }

    @Operation(summary = "회원 탈퇴")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal String userId,
                                              // TODO: 프론트 연동 시 @RequestBody DeleteAccountRequest req 로 변경
                                              // 현재 Swagger 테스트용으로 @RequestParam 사용 (DELETE + RequestBody Swagger 미지원)
                                              @RequestParam(required = false) String password) {
        userService.deleteAccount(userId, password);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "소셜 계정 목록 조회")
    @GetMapping("/me/social-accounts")
    public ResponseEntity<List<SocialAccountResponse>> getSocialAccounts(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getSocialAccounts(userId));
    }

    @Operation(summary = "소셜 계정 연동 해제")
    @DeleteMapping("/me/social-accounts/{provider}")
    public ResponseEntity<Void> unlinkSocialAccount(
            @AuthenticationPrincipal String userId,
            @PathVariable String provider) {
        userService.unlinkSocialAccount(userId, provider);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/me/notifications/settings")
    public ResponseEntity<NotificationResponse> getNotificationSettings(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getNotificationSettings(userId));
    }

    @Operation(summary = "알림 설정 수정")
    @PutMapping("/me/notifications/settings")
    public ResponseEntity<NotificationResponse> updateNotificationSettings(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody NotificationUpdateRequest req) {
        return ResponseEntity.ok(userService.updateNotificationSettings(userId, req));
    }

}
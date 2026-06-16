package com.example.kuriq.controller;


import com.example.kuriq.dto.course.response.NextCourseResponse;
import com.example.kuriq.dto.notification.request.NotificationUpdateRequest;
import com.example.kuriq.dto.notification.response.NotificationResponse;
import com.example.kuriq.dto.user.request.DeleteAccountRequest;
import com.example.kuriq.dto.user.request.UpdateEmailRequest;
import com.example.kuriq.dto.user.request.UserProfileUpdateRequest;
import com.example.kuriq.dto.user.response.*;
import com.example.kuriq.service.RecommendationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.example.kuriq.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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
    private final RecommendationService recommendationService; // 다음 추천 강좌

    @Operation(summary = "내 정보 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(userId)));
    }

    @Operation(summary = "내 정보 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UserProfileUpdateRequest req) {
        return ResponseEntity.ok(UserResponse.from(userService.updateProfile(userId, req)));
    }

    @Operation(summary = "이메일 주소 수정", description = "소셜 로그인 사용자가 이메일 알림을 위해 이메일 주소를 등록합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/me/email")
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateEmailRequest req) {
        userService.updateEmail(userId, req.getEmail());
        return ResponseEntity.ok().build();
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

    // 이수 강좌 수, 총 학습 시간, 연속 학습일, 완료 로드맵 수 반환
    @Operation(summary = "학습 통계 조회",
            description = "이수 강좌 수 / 총 학습 시간 / 연속 학습일 / 완료 로드맵 수를 반환합니다.")
    @GetMapping("/me/stats")
    public ResponseEntity<UserStatsResponse> getStats(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getStats(userId));
    }

    // 카테고리별 이수 강좌 수 + 상대 진행률 반환
    // 가장 많이 이수한 카테고리 = 100% 기준으로 정규화
    @Operation(summary = "분야별 학습 현황",
            description = "카테고리별 이수 강좌 수와 상대 진행률(%)을 반환합니다.")
    @GetMapping("/me/stats/categories")
    public ResponseEntity<List<CategoryStatsResponse>> getCategoryStats(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(userService.getCategoryStats(userId));
    }

    // 이수 완료한 강좌 목록 최신순 페이징 반환
    @Operation(summary = "학습 이력 조회",
            description = "이수 완료한 강좌 목록을 최신순으로 페이징하여 반환합니다.")
    @GetMapping("/me/history")
    public ResponseEntity<List<LearningHistoryResponse>> getLearningHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(value = 50, message = "size는 최대 50까지 가능합니다") int size) {
        return ResponseEntity.ok(userService.getLearningHistory(userId, page, size));
    }

    @Operation(
            summary = "다음 추천 강좌 조회",
            description = "현재 보고 있는 주차의 baseCourseId를 기준으로 다음 단계 강좌를 추천합니다. " +
                    "baseCourseId가 없으면 가장 최근 이수 강좌 기준으로 추천합니다. " +
                    "이수 이력이 없거나 추천할 강좌가 없으면 204를 반환합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me/recommendations")
    public ResponseEntity<List<NextCourseResponse>> getRecommendation(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String roadmapId,
            @RequestParam(required = false) String baseCourseId) { // 현재 보고 있는 주차의 첫 번째 강좌 ID
        List<NextCourseResponse> result = recommendationService.getRecommendation(userId, roadmapId, baseCourseId);
        return result.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(result);
    }

}

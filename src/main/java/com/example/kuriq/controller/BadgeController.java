package com.example.kuriq.controller;

import com.example.kuriq.dto.badge.BadgeResponse;
import com.example.kuriq.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Badge", description = "뱃지 API")
@RestController
@RequestMapping("/api/v1/users/me/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    // 전체 뱃지 목록을 BadgeType 선언 순서대로 반환
    @Operation(
            summary = "내 뱃지 목록 조회",
            description = "전체 뱃지 목록을 반환합니다. 획득한 뱃지는 acquired=true, 미획득 뱃지는 acquired=false입니다."
    )
    @GetMapping
    public ResponseEntity<List<BadgeResponse>> getMyBadges(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(badgeService.getMyBadges(userId));
    }
}

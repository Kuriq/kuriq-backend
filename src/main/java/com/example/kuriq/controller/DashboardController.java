package com.example.kuriq.controller;

import com.example.kuriq.dto.dashboard.response.WeeklyDashboardResponse;
import com.example.kuriq.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dashboard", description = "학습 대시보드 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "주간 대시보드 조회",
            description = "특정 로드맵의 주차별 학습 현황을 조회합니다. weekNumber 생략 시 현재 주차를 자동 산정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 대시보드 조회 성공"),
            @ApiResponse(responseCode = "404", description = "로드맵을 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "로드맵 접근 권한 없음")
    })
    @GetMapping("/weekly")
    public ResponseEntity<WeeklyDashboardResponse> getWeekly(
            @Parameter(description = "로드맵 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String roadmapId,
            @Parameter(description = "주차 번호 (생략 시 현재 주차)", example = "3")
            @RequestParam(required = false) Integer weekNumber,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(dashboardService.getWeeklyDashboard(roadmapId, weekNumber, userId));
    }
}

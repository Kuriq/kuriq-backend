package com.example.kuriq.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/* 사용자 학습 통계 응답 DTO (이수 강좌 수, 총 학습 시간, 연속 학습일, 완료 로드맵 수) */

@Getter
@Builder
@Schema(description = "학습 통계 응답")
public class UserStatsResponse {

    // learning_history 테이블 COUNT(*) 결과
    @Schema(description = "총 이수 강좌 수", example = "12")
    private long totalCompletedCourses;

    // 이수한 강좌의 courses.estimated_hours 합산
    @Schema(description = "총 학습 시간(시간)", example = "36.5")
    private BigDecimal totalLearningHours;

    // learning_history.completed_at 날짜 기준 오늘부터 연속으로 학습한 일수
    @Schema(description = "현재 연속 학습 일수", example = "5")
    private int streakDays;

    // roadmaps 테이블에서 is_completed = true 카운트
    @Schema(description = "완료한 로드맵 수", example = "2")
    private long completedRoadmapCount;
}

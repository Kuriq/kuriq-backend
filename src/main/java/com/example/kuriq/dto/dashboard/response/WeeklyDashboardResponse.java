package com.example.kuriq.dto.dashboard.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
@Schema(description = "주간 대시보드 응답")
public class WeeklyDashboardResponse {

    @Schema(description = "로드맵 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String roadmapId;

    @Schema(description = "주차 번호", example = "3")
    private int weekNumber;

    @Schema(description = "주차 제목", example = "데이터 시각화 입문")
    private String weekTitle;

    @Schema(description = "주차 기간")
    private DateRange dateRange;

    @Schema(description = "해당 주차 총 강좌 수", example = "4")
    private int totalCourses;

    @Schema(description = "완료한 강좌 수", example = "2")
    private int completedCourses;

    @Schema(description = "진행률 (%)", example = "50")
    private int progressPercent;

    @Schema(description = "남은 학습 시간 (시간)", example = "3.0")
    private BigDecimal remainingHours;

    @Schema(description = "해당 주차 강좌 목록")
    private List<CourseItem> courses;

    @Schema(description = "큐릭 메시지")
    private KuriqMessage kuriqMessage;

    @Getter
    @Builder
    public static class DateRange {
        @Schema(description = "시작일", example = "2026-04-14")
        private String start;

        @Schema(description = "종료일", example = "2026-04-20")
        private String end;
    }

    @Getter
    @Builder
    public static class CourseItem {
        @Schema(description = "로드맵 항목 ID", example = "660e8400-e29b-41d4-a716-446655440000")
        private String itemId;

        @Schema(description = "강좌 ID", example = "770e8400-e29b-41d4-a716-446655440000")
        private String courseId;

        @Schema(description = "강좌명", example = "데이터 시각화 with Python")
        private String title;

        @Schema(description = "플랫폼", example = "K-MOOC")
        private String platform;

        @Schema(description = "난이도", example = "초급")
        private String difficulty;

        @Schema(description = "예상 학습 시간 (시간)", example = "3.0")
        private BigDecimal estimatedHours;

        @Schema(description = "완료 여부", example = "true")
        private boolean isCompleted;

        @Schema(description = "완료 시각", example = "2026-04-15T14:30:00+09:00")
        private OffsetDateTime completedAt;

        @Schema(description = "수강 신청 URL", example = "https://...")
        private String url;

        @Schema(description = "노트 작성 여부", example = "true")
        private boolean hasNote;
    }

    @Getter
    @Builder
    public static class KuriqMessage {
        @Schema(description = "표정", example = "wink")
        private String expression;

        @Schema(description = "응원 메시지", example = "절반 넘었어요! 잘 하고 있어요 🎉")
        private String text;
    }
}

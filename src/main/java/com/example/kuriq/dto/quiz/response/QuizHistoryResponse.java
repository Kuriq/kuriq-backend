package com.example.kuriq.dto.quiz.response;

import com.example.kuriq.entity.quiz.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Schema(description = "퀴즈 히스토리 응답")
@Getter
@Builder
public class QuizHistoryResponse {

    @Schema(description = "퀴즈 세션 목록")
    private List<Item> content;

    @Schema(description = "전체 요소 수", example = "15")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "2")
    private int totalPages;

    @Schema(description = "현재 페이지", example = "0")
    private int currentPage;

    public static QuizHistoryResponse from(org.springframework.data.domain.Page<Item> page) {
        return QuizHistoryResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Getter
    @Builder
    public static class Item {
        @Schema(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
        private String quizSessionId;

        @Schema(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String courseId;

        @Schema(description = "강좌명", example = "모두를 위한 파이썬")
        private String courseTitle;

        @Schema(description = "총 문제 수", example = "3")
        private Integer totalQuestions;

        @Schema(description = "맞힌 문제 수", example = "2")
        private Integer correctCount;

        @Schema(description = "점수 (%)", example = "66")
        private Integer scorePercent;

        @Schema(description = "제출 시각", example = "2026-04-15T14:30:00+09:00")
        private OffsetDateTime submittedAt;

        @Schema(description = "생성 시각", example = "2026-04-15T14:00:00+09:00")
        private OffsetDateTime createdAt;

        public static Item from(QuizSession session, String courseTitle) {
            return Item.builder()
                    .quizSessionId(session.getId())
                    .courseId(session.getCourseId())
                    .courseTitle(courseTitle)
                    .totalQuestions(session.getTotalQuestions())
                    .correctCount(session.getSubmittedCorrectCount())
                    .scorePercent(session.getTotalQuestions() != null && session.getTotalQuestions() > 0
                            ? (session.getSubmittedCorrectCount() != null ? session.getSubmittedCorrectCount() * 100 / session.getTotalQuestions() : 0)
                            : 0)
                    .submittedAt(session.getSubmittedAt() != null
                            ? session.getSubmittedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()
                            : null)
                    .createdAt(session.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                    .build();
        }
    }
}

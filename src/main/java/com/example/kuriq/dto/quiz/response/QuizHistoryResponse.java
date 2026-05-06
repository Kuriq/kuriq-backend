package com.example.kuriq.dto.quiz.response;

import com.example.kuriq.entity.quiz.QuizSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
@Schema(description = "퀴즈 이력 조회 응답")
public class QuizHistoryResponse {
    @Schema(description = "퀴즈 이력 목록")
    private List<Item> content;
    @Schema(description = "전체 이력 수", example = "12")
    private long totalElements;
    @Schema(description = "전체 페이지 수", example = "2")
    private int totalPages;
    @Schema(description = "현재 페이지", example = "0")
    private int currentPage;
    @Schema(description = "페이지 크기", example = "10")
    private int size;
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    public static QuizHistoryResponse from(Page<Item> page) {
        return QuizHistoryResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "퀴즈 이력 항목")
    public static class Item {
        @Schema(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
        private String quizSessionId;
        @Schema(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        private String courseId;
        @Schema(description = "강좌 제목", example = "모두를 위한 파이썬")
        private String courseTitle;
        @Schema(description = "점수 비율", example = "80")
        private int scorePercent;
        @Schema(description = "정답 수", example = "4")
        private int correctCount;
        @Schema(description = "전체 문항 수", example = "5")
        private int totalQuestions;
        @Schema(description = "퀴즈 생성 시각", example = "2026-04-15T14:00:00+09:00")
        private OffsetDateTime createdAt;

        public static Item from(QuizSession session, String courseTitle) {
            int totalQuestions = session.getTotalQuestions() == null ? 0 : session.getTotalQuestions();
            int correctCount = session.getCorrectCount() == null ? 0 : session.getCorrectCount();
            int scorePercent = totalQuestions <= 0 ? 0 : (correctCount * 100) / totalQuestions;
            OffsetDateTime createdAt = session.getCreatedAt()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toOffsetDateTime();

            return Item.builder()
                    .quizSessionId(session.getId())
                    .courseId(session.getCourseId())
                    .courseTitle(courseTitle)
                    .scorePercent(scorePercent)
                    .correctCount(correctCount)
                    .totalQuestions(totalQuestions)
                    .createdAt(createdAt)
                    .build();
        }
    }
}

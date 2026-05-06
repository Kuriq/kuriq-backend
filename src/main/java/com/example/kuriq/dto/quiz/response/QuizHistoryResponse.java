package com.example.kuriq.dto.quiz.response;

import com.example.kuriq.entity.quiz.QuizSession;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class QuizHistoryResponse {
    private List<Item> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
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
    public static class Item {
        private String quizSessionId;
        private String courseId;
        private String courseTitle;
        private int scorePercent;
        private int correctCount;
        private int totalQuestions;
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

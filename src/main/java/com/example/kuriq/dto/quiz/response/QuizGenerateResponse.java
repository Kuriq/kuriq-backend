package com.example.kuriq.dto.quiz.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizGenerateResponse {
    private String quizSessionId;
    private String courseId;
    private String noteId;
    private List<QuestionDto> questions;

    @Getter
    @Builder
    public static class QuestionDto {
        private String questionId;
        private String type;
        private String question;
        private List<OptionDto> options;
    }

    @Getter
    @Builder
    public static class OptionDto {
        private String id;
        private String text;
    }
}

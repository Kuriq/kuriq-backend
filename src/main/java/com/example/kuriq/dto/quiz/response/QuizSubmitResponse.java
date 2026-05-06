package com.example.kuriq.dto.quiz.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizSubmitResponse {
    private String quizSessionId;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer scorePercent;
    private List<ResultDto> results;
    private String quriMessage;
    private List<String> weakTopics;

    @Getter
    @Builder
    public static class ResultDto {
        private String questionId;
        private String type;
        private Boolean isCorrect;
        private String result;
        private Object userAnswer;
        private String correctAnswer;
        private String explanation;
        private String feedback;
        private String noteReference;
        private String weakTopic;
    }
}

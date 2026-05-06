package com.example.kuriq.dto.quiz.response;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResultDto {
        private String questionId;
        private String type;
        private Boolean isCorrect;
        private String result;
        private Object userAnswer;
        private Object correctAnswer;
        private String explanation;
        private String feedback;
        private String noteReference;
        private String weakTopic;
    }
}

package com.example.kuriq.dto.quiz.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Schema(description = "퀴즈 제출 응답")
@Getter
@Builder
public class QuizSubmitResponse {

    @Schema(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String quizSessionId;

    @Schema(description = "총 문제 수", example = "3")
    private Integer totalQuestions;

    @Schema(description = "맞힌 문제 수", example = "2")
    private Integer correctCount;

    @Schema(description = "점수 (%)", example = "66")
    private Integer scorePercent;

    @Schema(description = "문제별 채점 결과")
    private List<ResultDto> results;

    @Schema(description = "큐리 메시지", example = "절반 이상 맞혔어요!")
    private String quriMessage;

    @Schema(description = "취약 주제 목록")
    private List<String> weakTopics;

    @Getter
    @Builder
    public static class ResultDto {
        @Schema(description = "문제 ID")
        private String questionId;

        @Schema(description = "문제 유형")
        private String type;

        @Schema(description = "정답 여부")
        private Boolean isCorrect;

        @Schema(description = "채점 결과 (CORRECT, PARTIAL, WRONG, GRADING_FAILED)")
        private String result;

        @Schema(description = "사용자 답안")
        private Object userAnswer;

        @Schema(description = "정답")
        private Object correctAnswer;

        @Schema(description = "해설")
        private String explanation;

        @Schema(description = "피드백")
        private String feedback;

        @Schema(description = "노트 참조 내용")
        private String noteReference;

        @Schema(description = "취약 주제")
        private String weakTopic;
    }
}

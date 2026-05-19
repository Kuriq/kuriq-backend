package com.example.kuriq.dto.quiz.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Schema(description = "퀴즈 답안 제출 요청")
@Getter
public class QuizSubmitRequest {

    @Schema(description = "답안 목록")
    @NotEmpty(message = "모든 문제에 답변해 주세요")
    @Valid
    private List<AnswerDto> answers;

    @Getter
    @Schema(description = "개별 문제 답안")
    public static class AnswerDto {
        @Schema(description = "문제 ID", example = "770e8400-e29b-41d4-a716-446655440001")
        private String questionId;

        @Schema(description = "답안 (객관식: 문자열, OX: boolean, 단답형: 문자열)")
        private Object answer;
    }
}

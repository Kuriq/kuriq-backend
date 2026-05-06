package com.example.kuriq.dto.quiz.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Schema(description = "퀴즈 답안 제출 요청")
@Getter
public class QuizSubmitRequest {

    @Schema(description = "제출 답안 목록")
    @Valid
    @NotEmpty(message = "answers를 입력해 주세요")
    private List<@NotNull(message = "answer 항목을 입력해 주세요") @Valid AnswerDto> answers;

    @Getter
    @Schema(description = "문항별 제출 답안")
    public static class AnswerDto {
        @Schema(description = "문항 ID", example = "770e8400-e29b-41d4-a716-446655440001")
        @NotNull(message = "questionId를 입력해 주세요")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "questionId는 UUID 형식이어야 합니다")
        private String questionId;

        @NotNull(message = "answer를 입력해 주세요")
        @Schema(description = "문항 유형별 답안. 객관식은 문자열(A/B/C/D), OX는 Boolean, 단답형은 문자열", example = "B")
        private Object answer;

        @JsonIgnore
        public String normalizedQuestionId() {
            return questionId;
        }
    }
}

package com.example.kuriq.dto.quiz.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Getter
public class QuizSubmitRequest {

    @Valid
    @NotEmpty(message = "answers를 입력해 주세요")
    private List<@NotNull(message = "answer 항목을 입력해 주세요") @Valid AnswerDto> answers;

    @Getter
    public static class AnswerDto {
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "questionId를 입력해 주세요")
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "questionId는 UUID 형식이어야 합니다")
        private String questionId;

        @NotNull(message = "answer를 입력해 주세요")
        private Object answer;

        @JsonIgnore
        public String normalizedQuestionId() {
            return questionId;
        }
    }
}

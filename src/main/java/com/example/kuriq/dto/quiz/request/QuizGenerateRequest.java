package com.example.kuriq.dto.quiz.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Schema(description = "퀴즈 생성 요청")
@Getter
public class QuizGenerateRequest {

    @Schema(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "noteId를 입력해 주세요")
    private String noteId;

    @Schema(description = "제외할 퀴즈 세션 ID 목록")
    private List<String> excludeSessionIds;
}

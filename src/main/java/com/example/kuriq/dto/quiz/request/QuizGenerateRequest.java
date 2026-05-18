package com.example.kuriq.dto.quiz.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.List;

@Schema(description = "퀴즈 생성 요청")
@Getter
public class QuizGenerateRequest {

    @Schema(description = "퀴즈를 생성할 기반 노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "noteId를 입력해 주세요")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "noteId는 UUID 형식이어야 합니다")
    private String noteId;

    @Schema(description = "중복 방지용 이전 퀴즈 세션 ID 목록. 테스트 시에는 비워두거나 생략하세요.", example = "[]")
    private List<@Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "excludeSessionIds는 UUID 형식이어야 합니다") String> excludeSessionIds;
}

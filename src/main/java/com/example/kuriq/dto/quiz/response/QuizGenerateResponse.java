package com.example.kuriq.dto.quiz.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "퀴즈 생성 응답")
public class QuizGenerateResponse {
    @Schema(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String quizSessionId;
    @Schema(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String courseId;
    @Schema(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private String noteId;
    @Schema(description = "생성된 퀴즈 문항 목록")
    private List<QuestionDto> questions;

    @Getter
    @Builder
    @Schema(description = "퀴즈 문항")
    public static class QuestionDto {
        @Schema(description = "문항 ID", example = "770e8400-e29b-41d4-a716-446655440001")
        private String questionId;
        @Schema(description = "문항 유형", example = "MULTIPLE_CHOICE")
        private String type;
        @Schema(description = "문항 내용", example = "노트에서 정리한 내용 중, 파이썬에서 변수를 생성할 때 필요한 것은?")
        private String question;
        @Schema(description = "객관식 선택지. TRUE_FALSE/SHORT_ANSWER는 null")
        private List<OptionDto> options;
    }

    @Getter
    @Builder
    @Schema(description = "객관식 선택지")
    public static class OptionDto {
        @Schema(description = "선택지 ID", example = "A")
        private String id;
        @Schema(description = "선택지 내용", example = "값 할당만으로 생성")
        private String text;
    }
}

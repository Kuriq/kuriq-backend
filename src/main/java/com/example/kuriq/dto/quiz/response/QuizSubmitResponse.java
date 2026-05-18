package com.example.kuriq.dto.quiz.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Builder
@Schema(description = "퀴즈 답안 제출 응답")
public class QuizSubmitResponse {
    @Schema(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String quizSessionId;
    @Schema(description = "전체 문항 수", example = "5")
    private Integer totalQuestions;
    @Schema(description = "정답 수", example = "4")
    private Integer correctCount;
    @Schema(description = "점수 비율", example = "80")
    private Integer scorePercent;
    @Schema(description = "문항별 채점 결과")
    private List<ResultDto> results;
    @Schema(description = "전체 결과에 대한 큐리 메시지", example = "5문제 중 4개를 맞혔어요! '자료형 명칭' 부분을 노트에서 한 번 더 확인해 보세요.")
    private String quriMessage;
    @Schema(description = "취약 주제 목록", example = "[\"자료형 명칭\"]")
    private List<String> weakTopics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "문항별 채점 결과")
    public static class ResultDto {
        @Schema(description = "문항 ID", example = "770e8400-e29b-41d4-a716-446655440003")
        private String questionId;
        @Schema(description = "문항 유형", example = "SHORT_ANSWER")
        private String type;
        @Schema(description = "정답 여부. PARTIAL/GRADING_FAILED는 false", example = "false")
        private Boolean isCorrect;
        @Schema(description = "단답형 채점 결과", example = "PARTIAL")
        private String result;
        @Schema(description = "사용자 답안", example = "리스트(list)")
        private Object userAnswer;
        @Schema(description = "정답", example = "리스트")
        private Object correctAnswer;
        @Schema(description = "해설", example = "여러 값을 순서대로 저장하고 수정 가능한 대표 자료형은 리스트입니다.")
        private String explanation;
        @Schema(description = "단답형 PARTIAL/WRONG/GRADING_FAILED 피드백", example = "거의 맞았어요! 정확한 명칭은 리스트입니다.")
        private String feedback;
        @Schema(description = "노트 참조 문장", example = "리스트: 순서 있음, 수정 가능(mutable)")
        private String noteReference;
        @Schema(description = "취약 주제", example = "자료형 명칭")
        private String weakTopic;
    }
}

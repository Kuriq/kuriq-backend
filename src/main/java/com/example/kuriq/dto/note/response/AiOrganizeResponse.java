package com.example.kuriq.dto.note.response;

import com.example.kuriq.client.AiClient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "AI 노트 정리 응답")
public class AiOrganizeResponse {

    @Schema(description = "추출된 키워드 목록", example = "[\"변수\", \"자료형\", \"리스트\", \"딕셔너리\", \"반복문\"]")
    private List<String> keywords;

    @Schema(description = "구조화된 요약 (마크다운 형식)", example = "### 파이썬 기초 문법\n- 변수: 타입 선언 없이 값 할당으로 생성\n...")
    private String structuredSummary;

    @Schema(description = "학습 제안 목록")
    private List<String> suggestions;

    public static AiOrganizeResponse from(AiClient.OrganizeAiResponse aiResponse) {
        return AiOrganizeResponse.builder()
                .keywords(aiResponse.getKeywords())
                .structuredSummary(aiResponse.getStructuredSummary())
                .suggestions(aiResponse.getSuggestions())
                .build();
    }
}

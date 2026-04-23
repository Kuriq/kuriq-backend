package com.example.kuriq.dto.roadmap.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 로드맵 생성 요청 DTO.
 *
 * API:
 * - POST /api/v1/roadmap/generate
 *
 * 역할:
 * - 사용자가 입력한 학습 목표 및 조건을 서버로 전달
 * - 해당 입력은 AI 로드맵 생성의 프롬프트로 사용됨
 *
 * 요청 예시:
 * {
 *   "prompt": "파이썬 기초부터 데이터 분석까지 배우고 싶어요. 주 5시간 가능합니다."
 * }
 *
 * 검증 규칙:
 * - 필수 입력 값 (공백 불가)
 * - 최소 10자 이상 (의미 있는 목표 입력 유도)
 * - 최대 500자 이하 (과도한 입력 제한)
 */
@Getter
public class RoadmapGenerateRequest {

    // swagger request body 지정
    @Schema(example = "파이썬 기초부터 데이터 분석까지 배우고 싶어요. 주 5시간 가능합니다.")
    // 사용자의 학습 목표 및 조건을 포함한 자연어 입력
    // 해당 값은 AI에게 전달되어 개인 맞춤형 학습 로드맵 생성에 사용됨
    @NotBlank(message = "학습 목표를 입력해 주세요")
    @Size(min = 10, max = 500, message = "10자 이상 500자 이하로 입력해 주세요")
    private String prompt;
}

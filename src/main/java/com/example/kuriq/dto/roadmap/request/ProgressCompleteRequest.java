package com.example.kuriq.dto.roadmap.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 학습 완료 처리 요청 DTO.
 *
 * API:
 * - POST /api/v1/progress/complete
 *
 * 역할:
 * - 사용자가 특정 로드맵 항목(강좌)을 완료했음을 서버에 전달
 * - 해당 요청을 기반으로 학습 상태 및 진행률이 업데이트됨
 *
 * 요청 예시:
 * {
 *   "roadmapItemId": "uuid-of-item"
 * }
 *
 * 처리 흐름:
 * - RoadmapItem.isCompleted = true 변경
 * - LearningHistory 생성 (학습 이력 기록)
 * - 진행률 재계산 (전체/주차별)
 *
 * 검증 규칙:
 * - roadmapItemId: 필수 값 (공백 불가)
 */
@Getter
public class ProgressCompleteRequest {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    // 해당 ID를 기반으로 RoadmapItem을 조회하여 완료 상태로 변경하고 학습 이력을 생성
    @NotBlank(message = "로드맵 항목 ID를 입력해 주세요")
    private String roadmapItemId;  // 완료 처리할 로드맵 항목 ID
}
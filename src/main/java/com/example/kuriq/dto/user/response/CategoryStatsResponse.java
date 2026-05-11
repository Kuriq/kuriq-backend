package com.example.kuriq.dto.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "분야별 학습 현황 응답")
public class CategoryStatsResponse {

    // 사용자가 가장 많이 학습한 분야
    // courses 테이블의 category 값을 기준으로 집계
    @Schema(description = "카테고리명", example = "프로그래밍")
    private String category;

    // 해당 카테고리에서 완료한 강좌 개수
    // learning_history 기준으로 COUNT(*) 집계 결과
    @Schema(description = "이수 강좌 수", example = "4")
    private long completedCount;

    // 사용자가 어떤 분야를 주로 학습했는지 보여주기 위한 상대 비율
    // 계산 방식:
    // (현재 카테고리 이수 수 / 가장 많이 이수한 카테고리 수) * 100
    @Schema(description = "상대 진행률(%)", example = "80.0")
    private double progressPercent;
}

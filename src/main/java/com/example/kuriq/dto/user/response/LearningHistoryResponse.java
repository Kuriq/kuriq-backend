package com.example.kuriq.dto.user.response;

import com.example.kuriq.entity.roadmap.LearningHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/* 학습 이력 목록 응답 DTO. 강좌명, 플랫폼, 이수일 포함 */

@Getter
@Builder
@Schema(description = "학습 이력 응답")
public class LearningHistoryResponse {

    // 학습 이력 ID
    @Schema(description = "이력 ID", example = "uuid-5678")
    private String id;

    // 이수한 강좌 ID (Course 엔티티 참조용)
    @Schema(description = "강좌 ID", example = "uuid-course-1")
    private String courseId;

    // 프론트에서 바로 강좌명을 보여주기 위한 값
    @Schema(description = "강좌명", example = "파이썬 기초")
    private String courseTitle;

    // 강좌 출처 플랫폼
    // 예: K-MOOC, KOCW, 서울시 평생학습포털 등
    @Schema(description = "플랫폼", example = "K-MOOC")
    private String platform;

    // 강좌 분야 정보
    // 마이페이지 학습 통계나 카테고리 분석에 활용 가능
    // TODO: 카테고리는 빼도 됨. 그냥 추가한거
    @Schema(description = "카테고리", example = "프로그래밍")
    private String category;

    // 사용자가 해당 강좌를 완료 처리한 시각
    @Schema(description = "이수 완료 시각", example = "2026-04-16T10:00:00")
    private LocalDateTime completedAt;

    // 어떤 로드맵을 통해 학습했는지 기록하기 위한 값
    // 로드맵 삭제 후에도 이력은 남겨야 하므로 FK 대신 값만 저장
    @Schema(description = "출처 로드맵 ID", example = "uuid-roadmap-1")
    private String sourceRoadmapId;

    // Entity → DTO 변환 메서드
    // courseTitle/platform/category는 LearningHistory 테이블에 없어서
    // Service에서 Course 조회 후 같이 넘겨받음
    public static LearningHistoryResponse from(LearningHistory h,
                                               String courseTitle,
                                               String platform,
                                               String category) {

        return LearningHistoryResponse.builder()
                .id(h.getId())
                .courseId(h.getCourseId())
                .courseTitle(courseTitle)
                .platform(platform)
                .category(category)
                .completedAt(h.getCompletedAt())
                .sourceRoadmapId(h.getSourceRoadmapId())
                .build();
    }
}

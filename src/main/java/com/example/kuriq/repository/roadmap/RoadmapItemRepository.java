package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.RoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 로드맵 항목(RoadmapItem, 강좌 단위) 저장소.
 *
 * 역할:
 * - 주차별 강좌 목록 조회
 * - 학습 완료 여부 관리
 * - 진행률 계산을 위한 집계 쿼리 제공
 *
 * ERD 기준:
 * - roadmap_items는 특정 로드맵 + 주차에 속한 실제 학습 단위(강좌)
 * - isCompleted 값을 기반으로 진행률 및 통계 계산
 *
 * 사용 흐름:
 * - 사용자가 강좌 완료 체크 → ProgressService.completeItem()
 * - 완료 상태 변경 후, 아래 count 쿼리로 진행률 계산
 */
public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, String> {

    // 특정 로드맵의 특정 주차에 속한 강좌 목록을 주차 내 순서(orderInWeek) 기준으로 조회
    // 사용처: 주차 상세 화면 (강좌 리스트)
    List<RoadmapItem> findByRoadmapIdAndWeekNumberOrderByOrderInWeekAsc(
            String roadmapId, int weekNumber);

    // 해당 로드맵에서 완료된 강좌 수를 조회
    // 사용처: 전체 진행률 계산 (완료 수 / 전체 수)
    long countByRoadmapIdAndIsCompletedTrue(String roadmapId);

    // 해당 로드맵에서 미완료 강좌 수를 조회
    // 사용처: 남은 학습량 표시
    long countByRoadmapIdAndIsCompletedFalse(String roadmapId);

    // 특정 주차에 포함된 전체 강좌 수를 조회
    // 사용처: 주차별 진행률 계산
    long countByRoadmapIdAndWeekNumber(String roadmapId, int weekNumber);

    // 특정 주차에서 완료된 강좌 수를 조회한다.
    // 사용처: 주차별 진행률 계산 (완료 수 / 전체 수)
    long countByRoadmapIdAndWeekNumberAndIsCompletedTrue(String roadmapId, int weekNumber);

    // 동일 로드맵 내에서 특정 강좌(courseId)가 이미 존재하는지 확인
    // 사용처: AI 추천/생성 시 중복 강좌 방지
    boolean existsByRoadmapIdAndCourseId(String roadmapId, String courseId);
}

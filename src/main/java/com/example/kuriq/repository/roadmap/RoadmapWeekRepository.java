package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.RoadmapWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 로드맵 주차 메타데이터 저장소.
 * 대시보드 타임라인에서 주차 제목/설명/예상 학습시간을 조회할 때 사용.
 */
public interface RoadmapWeekRepository extends JpaRepository<RoadmapWeek, String> {
    // 특정 로드맵에 속한 모든 주차 메타데이터 전체를 weekNumber 기준 오름차순으로 조회한다.
    List<RoadmapWeek> findByRoadmapIdOrderByWeekNumberAsc(String roadmapId);

    // 특정 로드맵의 특정 주차 메타데이터를 조회한다.
    Optional<RoadmapWeek> findByRoadmapIdAndWeekNumber(String roadmapId, int weekNumber);
}

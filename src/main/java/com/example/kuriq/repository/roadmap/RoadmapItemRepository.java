package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.RoadmapItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, String> {
    
    List<RoadmapItem> findByRoadmapIdAndWeekNumberOrderByOrderInWeekAsc(
            String roadmapId, Integer weekNumber);
    
    // 전체 items 를 로드하기 위한 메서드 추가
    @Query("SELECT ri FROM RoadmapItem ri WHERE ri.roadmap.id = :roadmapId")
    List<RoadmapItem> findAllByRoadmapId(@Param("roadmapId") String roadmapId);
}

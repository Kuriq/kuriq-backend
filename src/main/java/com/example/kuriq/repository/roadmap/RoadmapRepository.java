package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 로드맵(Roadmap) 엔티티 접근 Repository.
 *
 * 역할:
 * - 사용자별 로드맵 조회 및 상태 관리
 * - active 로드맵 단일성 보장 로직 지원
 * - 알림/스케줄러용 사용자 조회 쿼리 제공
 *
 * 핵심 제약:
 * - userId 기준으로 isActive = true 인 로드맵은 반드시 1개만 존재해야 한다.
 * - 새로운 로드맵을 활성화하기 전, 기존 active 로드맵을 반드시 비활성화해야 한다.
 */
public interface RoadmapRepository extends JpaRepository<Roadmap, String> {

    // 현재 사용자가 진행 중인(active) 로드맵을 조회
    // userId + isActive = true 조건은 최대 1건이므로 Optional로 반환한다.
    // 사용처: 대시보드 기본 로드맵 조회, 알림 발송
    Optional<Roadmap> findByUserIdAndIsActiveTrue(String userId);

    // 용자의 전체 로드맵 목록을 생성일 기준 내림차순으로 조회
    // 사용처: 마이페이지 로드맵 히스토리
    List<Roadmap> findByUserIdOrderByCreatedAtDesc(String userId);

    // 사용자가 완료한 로드맵 개수를 조회
    // 사용처: 마이페이지 통계 (완료한 로드맵 수)
    long countByUserIdAndIsCompletedTrue(String userId);

    // 현재 활성화된(active) 로드맵을 모두 비활성화(활성화된 로드맵은 하나만 있어야함)
    // 주의: 새로운 로드맵을 activate 하기 직전에 반드시 호출해야 함
    @Modifying
    @Query("""
        UPDATE Roadmap r
        SET r.isActive = false
        WHERE r.userId = :userId
          AND r.isActive = true
        """)
    void deactivateCurrentRoadmap(@Param("userId") String userId);

    // 장기간 학습 활동이 없는 사용자 ID 목록을 조회한다.
    // 조건: 1. active 로드맵이 존재, 2. since 이후 LearningHistory가 없음
    // 사용처: 리마인드 알림 스케줄러
    @Query("""
        SELECT DISTINCT r.userId
        FROM Roadmap r
        WHERE r.isActive = true
          AND NOT EXISTS (
              SELECT 1
              FROM LearningHistory h
              WHERE h.userId = r.userId
                AND h.completedAt >= :since
          )
        """)
    List<String> findInactiveUserIds(@Param("since") LocalDateTime since);

    // 미완료 학습 항목이 존재하는 사용자 ID 목록을 조회한다.
    // 조건: 1. active 로드맵 존재, 2. 해당 로드맵에 isCompleted = false 인 아이템 존재
    // 사용처: 주간 학습 리마인드 알림
    @Query("""
        SELECT DISTINCT r.userId
        FROM Roadmap r
        JOIN r.items i
        WHERE r.isActive = true
          AND i.isCompleted = false
        """)
    List<String> findUsersWithIncompleteItems();

    // MultipleBagFetchException 에러 -> 한 번에 하나의 Bag만 fetch하기
    // 1차: weeks fetch
    @Query("""
    SELECT r FROM Roadmap r
    LEFT JOIN FETCH r.weeks
    WHERE r.id = :roadmapId
    """)
    Optional<Roadmap> findByIdWithWeeks(@Param("roadmapId") String roadmapId);

    // 2차: items + course fetch
    @Query("""
    SELECT r FROM Roadmap r
    LEFT JOIN FETCH r.items i
    LEFT JOIN FETCH i.course
    WHERE r.id = :roadmapId
    """)
    Optional<Roadmap> findByIdWithItems(@Param("roadmapId") String roadmapId);

}

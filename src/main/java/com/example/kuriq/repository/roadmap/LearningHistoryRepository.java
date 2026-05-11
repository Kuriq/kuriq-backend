package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.LearningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학습 이력(LearningHistory) 저장소.
 *
 * 역할:
 * - 사용자의 강좌 이수 기록 저장 및 조회
 * - 마이페이지 통계 및 학습 분석 데이터 제공
 *
 * ERD 기준:
 * - LearningHistory는 “사용자가 실제로 학습을 완료한 기록”을 저장하는 로그 테이블
 * - RoadmapItem(계획)과 달리, 실제 수행된 학습 데이터를 의미
 *
 * 주요 사용처:
 * - 마이페이지 통계 (이수 개수, 총 학습 시간 등)
 * - 학습 패턴 분석 (연속 학습 일수, 활동 여부)
 * - 리마인드 알림 기준 데이터
 */
public interface LearningHistoryRepository extends JpaRepository<LearningHistory, String> {

    // 사용자 이력 최신순 전체 조회
    // 연속 학습일 계산할 때 completedAt 날짜 뽑으려고 씀
    List<LearningHistory> findByUserIdOrderByCompletedAtDesc(String userId);

    // 사용자 이력 최신순 페이징 조회
    // GET /api/v1/users/me/history 페이지네이션용
    List<LearningHistory> findByUserIdOrderByCompletedAtDesc(String userId, Pageable pageable);

    // 이수한 강좌 총 개수
    // 마이페이지 통계 - 이수 강좌 수
    long countByUserId(String userId);

    // 특정 강좌 이수 여부 확인
    // 중복 이력 방지용
    boolean existsByUserIdAndCourseId(String userId, String courseId);

    // 이수 강좌들의 estimated_hours 합산
    // courses 테이블 JOIN해서 SUM
    // COALESCE: 이력 없으면 SUM이 null → 0으로 대체
    @Query("""
        SELECT COALESCE(SUM(c.estimatedHours), 0)
        FROM LearningHistory h
        JOIN Course c ON c.id = h.courseId
        WHERE h.userId = :userId
        """)
    BigDecimal sumEstimatedHoursByUserId(@Param("userId") String userId);

    // 카테고리별 이수 강좌 수 집계
    // 마이페이지 분야별 학습 현황용
    // category null인 강좌는 제외하고 집계
    @Query("""
        SELECT c.category, COUNT(h)
        FROM LearningHistory h
        JOIN Course c ON c.id = h.courseId
        WHERE h.userId = :userId
          AND c.category IS NOT NULL
        GROUP BY c.category
        ORDER BY COUNT(h) DESC
        """)
    List<Object[]> countByCategoryForUser(@Param("userId") String userId);

    // courseId 목록으로 이력 배치 조회
    List<LearningHistory> findByUserIdAndCourseIdIn(String userId, List<String> courseIds);
}
package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.LearningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 학습 이력(LearningHistory) 저장
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
    @Query(value = """
        SELECT COALESCE(SUM(c.estimated_hours), 0)
        FROM learning_history h
        JOIN courses c ON c.id = h.course_id
        WHERE h.user_id = :userId
        """, nativeQuery = true)
    BigDecimal sumEstimatedHoursByUserId(@Param("userId") String userId);

    // 카테고리별 이수 강좌 수 집계
    // 마이페이지 분야별 학습 현황용
    // category null인 강좌는 제외하고 집계
    @Query(value = """
        SELECT c.category, COUNT(h.id)
        FROM learning_history h
        JOIN courses c ON c.id = h.course_id
        WHERE h.user_id = :userId
          AND c.category IS NOT NULL
        GROUP BY c.category
        ORDER BY COUNT(h.id) DESC
        """, nativeQuery = true)
    List<Object[]> countByCategoryForUser(@Param("userId") String userId);

    // courseId 목록으로 이력 배치 조회
    List<LearningHistory> findByUserIdAndCourseIdIn(String userId, List<String> courseIds);

    // 뱃지 스트릭 계산용
    // BadgeService.calculateStreak() 에서 KST 날짜로 변환 후 연속 학습일 역산
    // 전체 completedAt 내림차순 반환 (엔티티가 아닌 LocalDateTime 만 조회해 불필요한 컬럼 로드 방지)
    @Query("""
        SELECT h.completedAt
        FROM LearningHistory h
        WHERE h.userId = :userId
        ORDER BY h.completedAt DESC
        """)
    List<LocalDateTime> findCompletedAtByUserIdOrderByDesc(@Param("userId") String userId);
}
package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.LearningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
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

    // 사용자의 학습 이력을 최신순으로 조회
    // 사용처: 마이페이지 최근 학습 기록 리스트
    List<LearningHistory> findByUserIdOrderByCompletedAtDesc(String userId);

    // 사용자의 학습 이력을 페이징하여 최신순으로 조회
    // 사용처: 무한 스크롤 / 페이지 기반 학습 기록 조회
    List<LearningHistory> findByUserIdOrderByCompletedAtDesc(String userId, Pageable pageable);

    // 사용자가 이수한 강좌의 총 개수를 조회
    // 사용처: 마이페이지 통계 (총 이수 강좌 수)
    long countByUserId(String userId);

    // 사용자가 특정 강좌를 이미 이수했는지 확인
    // 사용처: 중복 학습 이력 방지, 동일 강좌 재추천 방지
    boolean existsByUserIdAndCourseId(String userId, String courseId);
}
package com.example.kuriq.entity.roadmap;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * ERD: learning_history 테이블
 *
 * 사용자의 "실제 학습 완료 이력"을 저장하는 엔티티
 *
 * 역할:
 * - 어떤 사용자가 어떤 강좌를 언제 완료했는지 기록
 * - 로드맵과는 독립적으로 영구 보존 (삭제 영향 없음)
 *
 * 특징:
 * - roadmap_items는 "계획", learning_history는 "실제 이수"
 * - 강좌 완료 시 자동 생성됨 (서비스 레이어에서 처리)
 *
 * 데이터 설계 포인트:
 * - sourceRoadmapId / sourceRoadmapItemId는 FK가 아닌 값으로 저장
 *   → 로드맵 삭제 시에도 이력 보존 가능
 *   → 느슨한 연결 (Loose Coupling)
 *
 * 활용:
 * - 마이페이지 통계
 *   - 이수 강좌 수: COUNT(*)
 *   - 총 학습 시간: courses JOIN → SUM(estimated_hours)
 *   - 연속 학습 일수: completedAt 기준 날짜 계산
 */
@Entity
@Table(
        name = "learning_history",
        indexes = {
                // 사용자별 완료 시점 기준 조회 (최근 학습 순)
                @Index(name = "idx_history_user_completed", columnList = "userId, completedAt"),

                // 사용자 + 강좌 기준 조회 (중복 체크, 특정 강좌 이수 여부 확인)
                @Index(name = "idx_history_user_course", columnList = "userId, courseId"),

                // 특정 로드맵 기반 이수 이력 조회
                @Index(name = "idx_history_roadmap", columnList = "sourceRoadmapId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LearningHistory {

    // 이수 이력 ID (UUID)
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 사용자 ID
    @Column(nullable = false, length = 36)
    private String userId;

    // 이수한 강좌 ID
    @Column(nullable = false, length = 36)
    private String courseId;

    // 어떤 로드맵에서 이 강의를 완료했는지
    @Column(length = 36)
    private String sourceRoadmapId;

    // 로드맵 안에서 어떤 강의 항목을 통해 완료했는지
    @Column(length = 36)
    private String sourceRoadmapItemId;

    // 강좌 완료 시각(기본적으로 생성 시점 = 완료 시점)
    @Column(nullable = false)
    private LocalDateTime completedAt;

    // 최초 저장 시 실행(완료 시간을 현재 시각으로 자동 설정)
    @PrePersist
    private void prePersist() {
        completedAt = LocalDateTime.now();
    }

    // 이수 이력 생성 메서드
    public static LearningHistory create(String userId, String courseId,
                                         String roadmapId, String roadmapItemId) {
        LearningHistory h = new LearningHistory();
        h.userId = userId;
        h.courseId = courseId;
        h.sourceRoadmapId = roadmapId;
        h.sourceRoadmapItemId = roadmapItemId;
        return h;
    }
}
package com.example.kuriq.entity.roadmap;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * ERD: roadmap_items 테이블
 *
 * 로드맵 내 "개별 강좌 단위"를 나타내는 엔티티
 *
 * 역할:
 * - 어떤 강좌를
 * - 몇 주차에
 * - 어떤 순서로 학습할지 정의
 *
 * 특징:
 * - Roadmap : RoadmapItem = 1 : N
 * - Course : RoadmapItem = 1 : N
 * - 주차(weekNumber) + 순서(orderInWeek)로 학습 순서 결정
 *
 * 학습 상태 관리:
 * - isCompleted: 강좌 완료 여부
 * - completedAt: 완료 시각
 *
 * 추가:
 * - 강좌 완료 시 learning_history 테이블에도 이력 저장 (서비스 레이어에서 처리)
 */
@Entity
@Table(
        name = "roadmap_items",
        indexes = {
                // 특정 로드맵의 주차별 강좌 순서 조회 최적화
                @Index(name = "idx_items_roadmap_week_order",
                        columnList = "roadmap_id, weekNumber, orderInWeek"),

                // 강좌 기준 조회 (어떤 로드맵에서 사용되는지)
                @Index(name = "idx_items_course", columnList = "course_id"),

                // 로드맵 내 완료/미완료 강좌 조회 최적화
                @Index(name = "idx_items_roadmap_completed",
                        columnList = "roadmap_id, isCompleted")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapItem {

    // 강좌 항목 ID (UUID)
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 하나의 로드맵에 여러 강좌 항목 존재
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    // 하나의 강좌가 여러 로드맵에 포함될 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 주차 번호 (1주차, 2주차 ...)
    @Column(nullable = false)
    private Integer weekNumber;

    // 주차 내 학습 순서
    @Column(nullable = false)
    private Integer orderInWeek;

    // 강좌 완료 여부
    @Column(nullable = false)
    private Boolean isCompleted = false;

    // 강좌 완료 시각
    private LocalDateTime completedAt;

    // 강의 하나가 로드맵에 들어간 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 강의 하나가 로드맵에 들어간 시간 자동 설정
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // 로드맵 강좌 항목 생성
    public static RoadmapItem create(Roadmap roadmap, Course course,
                                     int weekNumber, int orderInWeek) {
        RoadmapItem item = new RoadmapItem();
        item.roadmap = roadmap;
        item.course = course;
        item.weekNumber = weekNumber;
        item.orderInWeek = orderInWeek;
        return item;
    }

    // 강좌 완료 처리(완료 상태 변경, 완료 시간 기록)
    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    // 강좌 완료 취소 (되돌리기)(완료 상태 해제, 완료 시각 초기화)
    public void uncomplete() {
        this.isCompleted = false;
        this.completedAt = null;
    }
}
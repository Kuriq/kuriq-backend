package com.example.kuriq.entity.roadmap;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ERD: roadmaps 테이블
 *
 * AI가 생성하는 개인 맞춤형 학습 로드맵 엔티티
 *
 * 특징:
 * - 사용자 자연어(prompt)를 기반으로 생성
 * - 주차별 학습 구조 (RoadmapWeek + RoadmapItem)
 * - 사용자당 동시에 1개의 활성 로드맵만 허용
 *
 * 상태 관리:
 * - isActive: 현재 진행 중 여부
 * - isCompleted: 전체 완료 여부
 * - activatedAt: 시작 시각
 * - completedAt: 완료 시각
 */
@Entity
@Table(
        name = "roadmaps",
        indexes = {
                // 사용자별 활성 로드맵 조회 최적화
                @Index(name = "idx_roadmaps_user_active", columnList = "userId, isActive"),

                // 사용자별 생성순 조회 (최근 로드맵)
                @Index(name = "idx_roadmaps_user_created", columnList = "userId, createdAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Roadmap {

    // 로드맵 ID (UUID)
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 로드맵 소유 사용자 ID
    @Column(nullable = false, length = 36)
    private String userId;

    // 사용자가 입력한 자연어 요청 (AI 입력값)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    // AI가 요약한 학습 목표
    @Column(nullable = false, length = 500)
    private String goal;

    //  전체 학습 기간 (주 단위)
    @Column(nullable = false)
    private Integer totalWeeks;

    // 주당 학습 시간
    @Column(nullable = false)
    private Integer weeklyHours;

    // 전체 강좌 수
    @Column(nullable = false)
    private Integer totalCourses;

    // 현재 활성 로드맵 여부
    // true = 진행 중
    // 사용자당 1개만 true
    @Column(nullable = false)
    private Boolean isActive = false;

    // 전체 완료 여부
    // 모든 RoadmapItem 완료 시 true
    @Column(nullable = false)
    private Boolean isCompleted = false;

    // 로드맵 시작 시각
    private LocalDateTime activatedAt;

    // 로드맵 완료 시각
    private LocalDateTime completedAt;

    // 생성 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Roadmap 1:N RoadmapItem
    // 로드맵을 구성하는 강의 목록
    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC, orderInWeek ASC")
    private List<RoadmapItem> items = new ArrayList<>();

    // Roadmap 1:N RoadmapWeek
    // 주차별 설명/구조
    @OneToMany(mappedBy = "roadmap", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC")
    private List<RoadmapWeek> weeks = new ArrayList<>();

    // Roadmap 객체가 DB에 처음 저장(INSERT)되기 직전에 실행되는 메서드
    // 생성/수정 시간을 현재 시간으로 자동설정
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 업데이트 시 실행
    // 수정 시간 갱신
    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 로드맵 생성 메서드
    public static Roadmap create(String userId, String prompt, String goal,
                                 int totalWeeks, int weeklyHours, int totalCourses) {
        Roadmap r = new Roadmap();
        r.userId = userId;
        r.prompt = prompt;
        r.goal = goal;
        r.totalWeeks = totalWeeks;
        r.weeklyHours = weeklyHours;
        r.totalCourses = totalCourses;
        return r;
    }

    // 학습 시작 버튼 클릭 -> 로드맵 활성화
    // 시작 시각 기록
    public void activate() {
        this.isActive = true;
        this.activatedAt = LocalDateTime.now();
    }

    // 로드맵 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 로드맵 완료 처리
    // 완료 상태 + 비활성화 + 완료 시간 기록
    public void complete() {
        this.isCompleted = true;
        this.isActive = false;
        this.completedAt = LocalDateTime.now();
    }

    // 현재 진행 중인 주차 계산
    // 아직 완료되지 않은 가장 작은 weekNumber 반환
    public int currentWeek() {
        return items.stream()
                .filter(i -> !i.getIsCompleted())
                .mapToInt(RoadmapItem::getWeekNumber)
                .min()
                .orElse(totalWeeks);
    }

    // 모든 강좌 완료 여부
    public boolean allItemsCompleted() {
        return !items.isEmpty() &&
                items.stream().allMatch(RoadmapItem::getIsCompleted);
    }
}
package com.example.kuriq.entity.roadmap;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

/**
 * 로드맵의 "주차 단위 메타데이터"를 관리하는 엔티티
 *
 * 역할:
 * - 각 주차의 학습 흐름 설명 제공
 * - 주차 제목, 설명, 예상 학습 시간을 저장
 *
 * 특징:
 * - 하나의 로드맵(Roadmap)에 여러 주차 존재 (1:N)
 * - (roadmap_id, weekNumber) 조합은 유니크 → 동일 로드맵 내 주차 중복 방지
 *
 * 사용 예:
 * - 대시보드 타임라인 UI
 * - "1주차: 파이썬 기초" 같은 구조 표시
 */
@Entity
@Table(
        name = "roadmap_weeks",
        indexes = {
                // 하나의 로드맵 내에서 weekNumber는 유일해야 함
                @Index(name = "uk_roadmap_week", columnList = "roadmap_id, weekNumber", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadmapWeek {

    /** 주차 ID (UUID) */
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    /**
     * 소속 로드맵 (N:1 관계)
     * - 여러 주차가 하나의 로드맵에 속함
     * - LAZY 로딩으로 성능 최적화
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    private Roadmap roadmap;

    /** 주차 번호 (1주차, 2주차, ...) */
    @Column(nullable = false)
    private Integer weekNumber;

    /**
     * AI가 생성한 주차 제목
     * 예: "파이썬 문법 기초"
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * AI가 생성한 주차별 학습 흐름 설명
     * (길어질 수 있으므로 TEXT 타입)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 해당 주차의 예상 학습 시간 (시간 단위)
     * 예: 12.5시간
     */
    @Column(precision = 4, scale = 1)
    private BigDecimal totalHours;

    // 주차 생성 메서드
    public static RoadmapWeek create(Roadmap roadmap, int weekNumber,
                                     String title, String description, BigDecimal totalHours) {
        RoadmapWeek rw = new RoadmapWeek();
        rw.roadmap = roadmap;   // 소속 로드맵
        rw.weekNumber = weekNumber;  // 주차 번호
        rw.title = title;  // 주차 제목
        rw.description = description;  // 주차 설명
        rw.totalHours = totalHours;  // 예상 학습 시간
        return rw;
    }
}

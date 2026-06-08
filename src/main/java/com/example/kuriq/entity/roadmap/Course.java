package com.example.kuriq.entity.roadmap;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERD: courses 테이블
 *
 * 외부 공공 교육 플랫폼(K-MOOC, KOCW 등)에서 수집한 강좌 정보를 저장하는 엔티티
 *
 * 역할:
 * - 로드맵에 포함될 강좌의 메타데이터 관리
 * - 사용자에게 추천 및 검색 기능 제공
 *
 * 특징:
 * - 하나의 강좌는 여러 로드맵에 포함될 수 있음 (1:N)
 * - 플랫폼별 고유 ID(platformCourseId)로 중복 방지
 *
 * 주요 기능:
 * - 강좌 검색 (카테고리, 난이도, 플랫폼)
 * - 로드맵 구성 (RoadmapItem에서 참조)
 */
@Entity
@Table(
        name = "courses",
        indexes = {
                // 플랫폼 + 강좌 ID 조합으로 중복 방지
                @Index(name = "uk_platform_course", columnList = "platform, platformCourseId", unique = true),

                // 카테고리 + 난이도 기준 검색 최적화
                @Index(name = "idx_courses_category_difficulty", columnList = "category, difficulty"),

                // 플랫폼별 조회 최적화
                @Index(name = "idx_courses_platform", columnList = "platform"),

                // 활성화된 강좌만 조회할 때 사용
                @Index(name = "idx_courses_active", columnList = "isActive")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Course {

    // 강좌 ID (UUID, 큐릭 내부 식별자)
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 강좌 제공 플랫폼 (K-MOOC, KOCW 등)
    @Column(nullable = false, length = 50)
    private String platform;

    // 원본 플랫폼에서의 강좌 ID
    @Column(nullable = false, length = 255)
    private String platformCourseId;

    // 강좌 제목
    @Column(nullable = false, length = 500)
    private String title;

    // 운영 기관/대학
    @Column(length = 255)
    private String institution;

    // 강좌 카테고리
    @Column(length = 100)
    private String category;

    // 난이도 (입문/초급/중급/심화)
    @Column(length = 10)
    private String difficulty;

    // 수강 기간 (주 단위)
    private Integer durationWeeks;

    // 예상 총 학습 시간
    @Column(precision = 5, scale = 1)
    private BigDecimal estimatedHours;

    // 수료증 제공 여부
    @Column(nullable = false)
    private Boolean hasCertificate = false;

    // 수강 신청 링크 (원본 플랫폼 URL)
    @Column(nullable = false, length = 1000)
    private String url;

    // 강좌 설명
    @Column(columnDefinition = "TEXT")
    private String description;

    // 강좌 활성 상태 (삭제 대신 비활성화)
    @Column(nullable = false)
    private Boolean isActive = true;

    // 마지막 데이터 수집 시각
    private LocalDateTime lastCrawledAt;

    // 크롤링해서 강좌 데이터가 DB에 들어간 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 강좌 데이터 업데이트 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 엔티티 최종 저장(INSERT) 직전에 실행(생성/수정 시간 자동 설정)
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 엔티티 수정(UPDATE) 직전에 실행(수정 시간 갱신)
    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

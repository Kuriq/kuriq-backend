package com.example.kuriq.dto.roadmap.response;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.RoadmapItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 로드맵 상세 응답 DTO.
 *
 * - 하나의 로드맵에 대한 전체 학습 구조를 계층 형태로 반환
 * - 프론트에서 대시보드(주차별 학습 계획 + 진행률)를 바로 렌더링할 수 있도록 설계
 *
 * - 계획(Week, Item) + 상태(progress, completed) + 메타데이터를 함께 제공
 * - 추가 API 호출 없이 화면 구성 가능 (API 최적화)
 *
 * 포함 정보:
 * - 로드맵 기본 정보 (goal, totalWeeks, weeklyHours 등)
 * - 전체 진행률 (progressPercent)
 * - 현재 진행 주차 (currentWeek)
 * - 생성/활성화/완료 시간
 */
@Getter
@Builder
public class RoadmapResponse {

    private String id;  // 로드맵 id

    private String title;  // 간략화된 로드맵 제목

    private String goal;  // 사용자의 학습 목표

    private String prompt;  // AI 생성에 사용된 원본 프롬프트

    private Integer totalWeeks;  // 총 주차 수

    private Integer weeklyHours;  // 주당 학습 시간

    private Integer totalCourses;  // 전체 강좌 수

    private Boolean isActive;  // 현재 활성화 여부

    private Boolean isCompleted;  // 전체 완료 여부

    private Integer currentWeek;  // 현재 진행 중인 주차

    private double progressPercent;  // 전체 진행률 (%)

    private LocalDateTime createdAt;  // 생성 시각

    private LocalDateTime activatedAt;  // 활성화 시각

    private LocalDateTime completedAt;  // 완료 시각

    private List<WeekResponse> weeks;  // 주차별 학습 구조

    /**
     * 주차 단위 응답 DTO.
     *
     * 역할:
     * - 해당 주차의 학습 계획 및 진행 상태 제공
     * - UI에서 “1주차, 2주차” 블록 단위 구성
     */
    @Getter
    @Builder
    public static class WeekResponse {

        private Integer weekNumber;  // 주차 번호

        private String title;  // 주차 제목 (예: "파이썬 기초")

        private String description;  // 주차 설명

        private BigDecimal totalHours;  // 해당 주차 총 예상 학습 시간

        private int completedCount;  // 완료된 강좌 수

        private int totalCount;  // 전체 강좌 수

        private double weekProgressPercent;  // 주차 진행률 (%)

        private List<RoadmapItemResponse> items;  // 해당 주차의 강좌 목록
    }

    /**
     * 강좌(학습 단위) 응답 DTO.
     *
     * 역할:
     * - 실제 학습 단위(강좌)와 완료 상태 제공
     * - 사용자 행동(완료 체크)의 대상
     */
    @Getter
    @Builder
    public static class RoadmapItemResponse {

        private String id;  // 항목 ID

        private Integer weekNumber;  // 주차 번호

        private Integer orderInWeek;  // 주차 내 순서

        private Boolean isCompleted;  // 완료 여부

        private LocalDateTime completedAt; // 완료 시각

        private CourseResponse course;  // 강좌 상세 정보

        // Entity → DTO 변환 메서드
        // RoadmapItem 엔티티를 응답 DTO로 변환하며, 내부적으로 CourseResponse도 함께 매핑한다.
        public static RoadmapItemResponse from(RoadmapItem item) {
            return RoadmapItemResponse.builder()
                    .id(item.getId())
                    .weekNumber(item.getWeekNumber())
                    .orderInWeek(item.getOrderInWeek())
                    .isCompleted(item.getIsCompleted())
                    .completedAt(item.getCompletedAt())
                    .course(CourseResponse.from(item.getCourse()))
                    .build();
        }

        /**
         * 강좌 정보 응답 DTO.
         *
         * 역할:
         * - 외부 교육 플랫폼(K-MOOC, KOCW 등)의 강좌 정보를 클라이언트에 전달
         * - 로드맵의 개별 학습 항목(RoadmapItem)에 포함되어 표시됨
         *
         * 사용 위치:
         * - RoadmapResponse → RoadmapItemResponse 내부
         *   → 사용자에게 실제 학습할 강좌 정보를 제공
         *
         * 특징:
         * - 강좌 메타데이터 중심 (제목, 플랫폼, 난이도 등)
         * - 수강 판단에 필요한 핵심 정보 포함 (기간, 예상 시간, 수료증 여부, URL)
         *
         * 목적:
         * - 사용자가 어떤 강의를 듣는지 직관적으로 이해하고
         * - 바로 수강 페이지로 이동할 수 있도록 지원
         */
        @Getter
        @Builder
        public static class CourseResponse {

            /**  */
            private String id;  // 강좌 ID

            private String title;  // 강좌 제목

            private String platform;  // 제공 플랫폼 (예: K-MOOC, KOCW)

            private String institution;  // 운영 기관 또는 대학

            private String category;  // 강좌 분야 (예: 프로그래밍, 데이터 분석 등)

            private String difficulty;  // 난이도 (입문 / 초급 / 중급 / 심화)

            private Integer durationWeeks;  // 수강 기간 (주 단위)

            private BigDecimal estimatedHours;  // 예상 학습 시간 (시간 단위)

            private Boolean hasCertificate;  // 수료증 제공 여부

            private String url;  // 수강 신청 또는 강의 페이지 URL

            /**
             * Entity → DTO 변환 메서드
             *
             * Course 엔티티를 클라이언트 응답용 DTO로 변환한다.
             */
            public static CourseResponse from(Course course) {
                return CourseResponse.builder()
                        .id(course.getId())
                        .title(course.getTitle())
                        .platform(course.getPlatform().name())
                        .institution(course.getInstitution())
                        .category(course.getCategory())
                        .difficulty(course.getDifficulty())
                        .durationWeeks(course.getDurationWeeks())
                        .estimatedHours(course.getEstimatedHours())
                        .hasCertificate(course.getHasCertificate())
                        .url(course.getUrl())
                        .build();
            }
        }
    }
}

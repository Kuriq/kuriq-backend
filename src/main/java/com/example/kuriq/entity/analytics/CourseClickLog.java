package com.example.kuriq.entity.analytics;

import com.example.kuriq.entity.roadmap.Platform;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 사용자가 수강신청 버튼을 클릭할 때마다 기록이 여기 쌓임
@Entity
@Table(
        name = "course_click_logs",
        indexes = {
                // 강좌별 + 시간순 조회 ("이 강좌는 언제 많이 클릭됐나?")
                @Index(name = "idx_clicks_course_time", columnList = "courseId, clickedAt"),

                // 플랫폼별 + 시간순 조회 ("K-MOOC vs KOCW 중 어디가 더 많이 클릭됐나?")
                @Index(name = "idx_clicks_platform_time", columnList = "platform, clickedAt"),

                // 시간순 전체 조회 ("오늘 클릭 로그 전체 보기")
                @Index(name = "idx_clicks_time", columnList = "clickedAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseClickLog {

    // 로그 ID (auto increment — 로그 테이블은 UUID보다 숫자 PK가 INSERT 성능이 좋음)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 클릭한 사용자 ID (비로그인 사용자는 null)
    @Column(length = 36)
    private String userId;

    // 클릭된 강좌 ID
    @Column(nullable = false, length = 36)
    private String courseId;

    // 강좌가 속한 플랫폼 (K_MOOC / KOCW / LLL_PORTAL / SEOUL_LLL)
    // Course.platform과 동일한 enum 사용
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    // 어느 화면에서 클릭했는지 (roadmap / search / dashboard / recommendation / history)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClickSource source;

    // 클릭한 시각
    @Column(nullable = false)
    private LocalDateTime clickedAt;

    // clickedAt을 DB에 저장하기 직전에 현재 시각으로 자동 세팅
    @PrePersist
    private void prePersist() {
        clickedAt = LocalDateTime.now();
    }

    // 클릭 로그 생성 메서드(필수값 누락 방지를 위해, 외부에서 new CourseClickLog() 직접 호출 불가하게 함)
    public static CourseClickLog create(String userId, String courseId,
                                        Platform platform, ClickSource source) {
        CourseClickLog log = new CourseClickLog();
        log.userId = userId;         // 비로그인이면 null
        log.courseId = courseId;
        log.platform = platform;
        log.source = source;
        return log;
    }

    // 어느 화면에서 클릭했는지를 나타내는 enum
    // ERD의 click_source_enum과 동일
    public enum ClickSource {
        roadmap,         // 로드맵 상세 화면
        search,          // 강좌 검색 화면
        dashboard,       // 주간 학습 대시보드
        recommendation,  // 큐리 추천 화면
        history          // 학습 이력 화면
    }
}

package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// Course 테이블 조회/저장 인터페이스
public interface CourseRepository extends JpaRepository<Course, String>, JpaSpecificationExecutor<Course> {

    // 플랫폼 + 플랫폼 강좌 ID로 조회 (크롤링 시 중복 방지용)
    Optional<Course> findByPlatformAndPlatformCourseId(Platform platform, String platformCourseId);

    @Modifying
    @Query(value = """
        INSERT INTO courses (
            id,
            platform,
            platform_course_id,
            title,
            institution,
            category,
            difficulty,
            duration_weeks,
            estimated_hours,
            has_certificate,
            url,
            description,
            is_active,
            last_crawled_at,
            created_at,
            updated_at
        ) VALUES (
            :id,
            :platform,
            :platformCourseId,
            :title,
            :institution,
            :category,
            :difficulty,
            :durationWeeks,
            :estimatedHours,
            :hasCertificate,
            :url,
            :description,
            :isActive,
            :lastCrawledAt,
            NOW(),
            NOW()
        )
        ON DUPLICATE KEY UPDATE
            title = VALUES(title),
            institution = VALUES(institution),
            category = VALUES(category),
            difficulty = VALUES(difficulty),
            duration_weeks = VALUES(duration_weeks),
            estimated_hours = VALUES(estimated_hours),
            has_certificate = VALUES(has_certificate),
            url = VALUES(url),
            description = VALUES(description),
            is_active = VALUES(is_active),
            last_crawled_at = VALUES(last_crawled_at),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertCourse(
            @Param("id") String id,
            @Param("platform") String platform,
            @Param("platformCourseId") String platformCourseId,
            @Param("title") String title,
            @Param("institution") String institution,
            @Param("category") String category,
            @Param("difficulty") String difficulty,
            @Param("durationWeeks") Integer durationWeeks,
            @Param("estimatedHours") java.math.BigDecimal estimatedHours,
            @Param("hasCertificate") Boolean hasCertificate,
            @Param("url") String url,
            @Param("description") String description,
            @Param("isActive") Boolean isActive,
            @Param("lastCrawledAt") java.time.LocalDateTime lastCrawledAt
    );

    // 활성화된 강좌만 조회
    List<Course> findByIsActiveTrue();

    // 카테고리별 조회
    List<Course> findByCategoryAndIsActiveTrue(String category);

    // AI 추천 실패 시 MySQL 폴백용 - 같은 카테고리에서 특정 강좌 제외하고 3개 조회 (정확히 일치)
    List<Course> findTop3ByCategoryAndIsActiveTrueAndIdNotOrderByIdAsc(
            String category, String excludeId);

    // 추천 폴백용 - category 컬럼에 alias 키워드가 포함된 강좌 조회 (LIKE 검색)
    // DB에 "외부연계\n\n\t\t인문/교양" 형태로 저장된 경우를 위해 별칭 포함 여부로 검색
    @Query(value = """
        SELECT * FROM courses
        WHERE is_active = true
          AND id != :excludeId
          AND category LIKE CONCAT('%', :alias, '%')
        ORDER BY id ASC
        LIMIT 10
        """, nativeQuery = true)
    List<Course> findTop10ByIsActiveTrueAndCategoryContainsAliasAndIdNot(
            @Param("alias") String alias,
            @Param("excludeId") String excludeId
    );
}

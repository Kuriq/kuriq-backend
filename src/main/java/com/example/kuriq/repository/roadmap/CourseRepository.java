package com.example.kuriq.repository.roadmap;

import com.example.kuriq.entity.roadmap.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Course 테이블 조회/저장 인터페이스
public interface CourseRepository extends JpaRepository<Course, String> {

    // 플랫폼 + 플랫폼 강좌 ID로 조회 (크롤링 시 중복 방지용)
    Optional<Course> findByPlatformAndPlatformCourseId(String platform, String platformCourseId);

    // 활성화된 강좌만 조회
    List<Course> findByIsActiveTrue();

    // 카테고리별 조회
    List<Course> findByCategoryAndIsActiveTrue(String category);
}

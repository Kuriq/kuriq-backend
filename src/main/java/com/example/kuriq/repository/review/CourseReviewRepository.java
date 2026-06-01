package com.example.kuriq.repository.review;

import com.example.kuriq.entity.review.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, String> {

    // 강좌별 리뷰 목록 — 최신순
    Page<CourseReview> findByCourseIdAndIsDeletedFalseOrderByCreatedAtDesc(String courseId, Pageable pageable);

    // 특정 사용자의 특정 강좌 리뷰 조회 — 1인 1리뷰 확인, 수정/삭제용
    Optional<CourseReview> findByUserIdAndCourseIdAndIsDeletedFalse(String userId, String courseId);

    // 1인 1리뷰 중복 체크
    boolean existsByUserIdAndCourseIdAndIsDeletedFalse(String userId, String courseId);

    // 내 리뷰 목록
    List<CourseReview> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);

    // 강좌 평균 별점 집계 — 삭제되지 않은 리뷰만
    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.courseId = :courseId AND r.isDeleted = false")
    Double calculateAverageRating(@Param("courseId") String courseId);

    // 강좌 리뷰 수 집계
    long countByCourseIdAndIsDeletedFalse(String courseId);
}

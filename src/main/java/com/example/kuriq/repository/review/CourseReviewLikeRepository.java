package com.example.kuriq.repository.review;

import com.example.kuriq.entity.review.CourseReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseReviewLikeRepository extends JpaRepository<CourseReviewLike, String> {

    boolean existsByReviewIdAndUserId(String reviewId, String userId);

    Optional<CourseReviewLike> findByReviewIdAndUserId(String reviewId, String userId);

    // 리뷰 좋아요 수 집계 — 뱃지 서비스에서 사용
    long countByUserId(String userId);
}


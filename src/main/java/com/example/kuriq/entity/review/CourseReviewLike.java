package com.example.kuriq.entity.review;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_review_likes",
        indexes = {
                @Index(name = "uk_review_like", columnList = "reviewId, userId", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CourseReviewLike {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 리뷰 ID (course_reviews 테이블 FK)
    @Column(nullable = false, length = 36)
    private String reviewId;

    // 좋아요 누른 사용자 ID (users 테이블 FK)
    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

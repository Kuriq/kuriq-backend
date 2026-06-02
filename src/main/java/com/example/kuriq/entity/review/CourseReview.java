package com.example.kuriq.entity.review;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_reviews",
        indexes = {
                @Index(name = "uk_review_user_course", columnList = "userId, courseId", unique = true), // 1인 1리뷰
                @Index(name = "idx_reviews_course_created", columnList = "courseId, createdAt"),
                @Index(name = "idx_reviews_course_rating", columnList = "courseId, rating") // AVG(rating) 집계용
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CourseReview {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 작성자 ID (users 테이블 FK)
    @Column(nullable = false, length = 36)
    private String userId;

    // 강좌 ID (courses 테이블 FK)
    @Column(nullable = false, length = 36)
    private String courseId;

    // 별점 1~5 (필수)
    @Column(nullable = false)
    private int rating;

    // 후기 본문 — 선택. 최대 1,000자
    @Column(columnDefinition = "TEXT")
    private String content;

    // 사전 지식 수준 — 선택. difficulty_match와 함께 표시
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PriorKnowledge priorKnowledge;

    // 난이도 체감 — 선택. prior_knowledge와 함께 표시
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DifficultyMatch difficultyMatch;

    // 리뷰 좋아요 수 캐시
    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    // 소프트 삭제 여부
    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /* ENUM */

    // 사전 지식 수준
    public enum PriorKnowledge {
        BEGINNER,     // 처음 접해봐요
        LITTLE,       // 조금 알아요
        INTERMEDIATE, // 어느 정도 알아요
        ADVANCED      // 잘 알아요
    }

    // 난이도 체감
    public enum DifficultyMatch {
        EASY, // 생각보다 쉬웠어요
        FIT,  // 난이도가 딱 맞았어요
        HARD  // 생각보다 어려웠어요
    }

    /* 생명주기 */

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* 비즈니스 메서드 */

    // 리뷰 수정 — 삭제된 리뷰는 수정 불가
    public void update(int rating, String content,
                       PriorKnowledge priorKnowledge, DifficultyMatch difficultyMatch) {
        if (this.isDeleted) throw new IllegalStateException("삭제된 리뷰는 수정할 수 없습니다.");
        this.rating = rating;
        this.content = content;
        this.priorKnowledge = priorKnowledge;
        this.difficultyMatch = difficultyMatch;
    }

    // 소프트 삭제
    public void softDelete() {
        this.isDeleted = true;
    }

    // 좋아요 수 증가
    public void increaseLikeCount() {
        this.likeCount++;
    }

    // 좋아요 수 감소
    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}

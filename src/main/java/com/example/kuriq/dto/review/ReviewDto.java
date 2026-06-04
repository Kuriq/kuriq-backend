package com.example.kuriq.dto.review;

import com.example.kuriq.entity.review.CourseReview;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewDto {

    // 페이지네이션 응답 래퍼
    @Getter
    @Builder
    public static class PageResponse {
        private List<ReviewResponse> content;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
    }

    /*** 요청 ***/

    @Getter
    public static class CreateRequest {
        @Min(value = 1, message = "별점은 최소 1점입니다.")
        @Max(value = 5, message = "별점은 최대 5점입니다.")
        private int rating;

        @Size(max = 1000, message = "후기는 최대 1,000자까지 입력할 수 있습니다.")
        private String content; // 선택

        private CourseReview.PriorKnowledge priorKnowledge; // 선택

        private CourseReview.DifficultyMatch difficultyMatch; // 선택

        private boolean anonymous;
    }

    @Getter
    public static class UpdateRequest {
        @Min(value = 1, message = "별점은 최소 1점입니다.")
        @Max(value = 5, message = "별점은 최대 5점입니다.")
        private int rating;

        @Size(max = 1000, message = "후기는 최대 1,000자까지 입력할 수 있습니다.")
        private String content; // 선택

        private CourseReview.PriorKnowledge priorKnowledge; // 선택

        private CourseReview.DifficultyMatch difficultyMatch; // 선택

        private boolean anonymous;
    }

    /*** 응답 ***/

    // 강좌 평점 요약 (상단 표시용)
    @Getter
    @Builder
    public static class SummaryResponse {
        private double averageRating; // 소수점 1자리 반올림
        private long reviewCount;
    }

    // 리뷰 상세
    @Getter
    @Builder
    public static class ReviewResponse {
        private String id;
        private String authorId;
        private String authorName;
        private boolean anonymous;
        private int rating;
        private String content;
        private CourseReview.PriorKnowledge priorKnowledge; // 사전 지식 수준
        private CourseReview.DifficultyMatch difficultyMatch; // 난이도 체감
        private int likeCount;
        private boolean likedByMe;
        private LocalDateTime createdAt;

        public static ReviewResponse from(CourseReview review, String authorName, boolean likedByMe) {
            return ReviewResponse.builder()
                    .id(review.getId())
                    .authorId(review.getUserId())
                    .authorName(review.isAnonymous() ? "익명" : authorName)
                    .anonymous(review.isAnonymous())
                    .rating(review.getRating())
                    .content(review.getContent())
                    .priorKnowledge(review.getPriorKnowledge())
                    .difficultyMatch(review.getDifficultyMatch())
                    .likeCount(review.getLikeCount())
                    .likedByMe(likedByMe)
                    .createdAt(review.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class LikeResponse {
        private boolean liked;
        private int likeCount;
    }
}

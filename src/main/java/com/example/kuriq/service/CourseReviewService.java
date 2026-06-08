package com.example.kuriq.service;

import com.example.kuriq.dto.review.ReviewDto;
import com.example.kuriq.entity.review.CourseReview;
import com.example.kuriq.entity.review.CourseReviewLike;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.review.CourseReviewLikeRepository;
import com.example.kuriq.repository.review.CourseReviewRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {

    private final CourseReviewRepository reviewRepository;
    private final CourseReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;

    // 강좌 평점 요약 조회
    public ReviewDto.SummaryResponse getSummary(String courseId) {
        Double avg = reviewRepository.calculateAverageRating(courseId);
        long count = reviewRepository.countByCourseIdAndIsDeletedFalse(courseId);

        // 소수점 1자리 반올림
        double averageRating = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
        return ReviewDto.SummaryResponse.builder()
                .averageRating(averageRating)
                .reviewCount(count)
                .build();
    }

    // 리뷰 목록 조회
    public ReviewDto.PageResponse getReviews(String courseId, String userId, int page, int size) {
        Page<CourseReview> reviewPage = reviewRepository
                .findByCourseIdAndIsDeletedFalseOrderByCreatedAtDesc(courseId, PageRequest.of(page, size));

        List<String> userIds = reviewPage.getContent().stream().map(CourseReview::getUserId).distinct().toList();
        Map<String, String> nameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<ReviewDto.ReviewResponse> content = reviewPage.getContent().stream()
                .map(r -> {
                    boolean likedByMe = userId != null &&
                            reviewLikeRepository.existsByReviewIdAndUserId(r.getId(), userId);
                    return ReviewDto.ReviewResponse.from(r, nameMap.getOrDefault(r.getUserId(), "알 수 없음"), likedByMe);
                })
                .toList();

        return ReviewDto.PageResponse.builder()
                .content(content)
                .currentPage(reviewPage.getNumber())
                .totalPages(reviewPage.getTotalPages())
                .totalElements(reviewPage.getTotalElements())
                .hasNext(reviewPage.hasNext())
                .build();
    }

    public Optional<ReviewDto.ReviewResponse> getMyReview(String userId, String courseId) {
        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");

        return reviewRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId)
                .map(review -> {
                    boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserId(review.getId(), userId);
                    return ReviewDto.ReviewResponse.from(review, authorName, likedByMe);
                });
    }

    // 리뷰 작성
    @Transactional
    public ReviewDto.ReviewResponse createReview(String userId, String courseId, ReviewDto.CreateRequest req) {
        // 1인 1리뷰 중복 체크
        if (reviewRepository.existsByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId)) {
            throw new IllegalStateException("이미 리뷰를 작성한 강좌입니다.");
        }

        CourseReview review = CourseReview.builder()
                .userId(userId)
                .courseId(courseId)
                .rating(req.getRating())
                .content(req.getContent())
                .anonymous(req.isAnonymous())
                .priorKnowledge(req.getPriorKnowledge())
                .difficultyMatch(req.getDifficultyMatch())
                .build();
        reviewRepository.save(review);

        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return ReviewDto.ReviewResponse.from(review, authorName, false);
    }

    // 내 리뷰 수정
    @Transactional
    public ReviewDto.ReviewResponse updateMyReview(String userId, String courseId, ReviewDto.UpdateRequest req) {
        CourseReview review = reviewRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        review.update(req.getRating(), req.getContent(), req.getPriorKnowledge(), req.getDifficultyMatch(), req.isAnonymous());
        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return ReviewDto.ReviewResponse.from(review, authorName, false);
    }

    // 내 리뷰 삭제
    @Transactional
    public void deleteMyReview(String userId, String courseId) {
        CourseReview review = reviewRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        review.softDelete();
        // 평균 별점 재계산은 DB 집계 쿼리(calculateAverageRating)가 매번 호출 시 처리함
    }

    // 리뷰 좋아요 토글
    @Transactional
    public ReviewDto.LikeResponse toggleLike(String userId, String reviewId) {
        CourseReview review = reviewRepository.findById(reviewId)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (reviewLikeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId)
                    .ifPresent(reviewLikeRepository::delete);
            review.decreaseLikeCount();
            return ReviewDto.LikeResponse.builder().liked(false).likeCount(review.getLikeCount()).build();
        } else {
            reviewLikeRepository.save(CourseReviewLike.builder()
                    .reviewId(reviewId).userId(userId).build());
            review.increaseLikeCount();
            badgeService.checkAndAwardOnCommunityActivity(review.getUserId());
            return ReviewDto.LikeResponse.builder().liked(true).likeCount(review.getLikeCount()).build();
        }
    }

    // 내 리뷰 목록
    public List<ReviewDto.ReviewResponse> getMyReviews(String userId) {
        List<CourseReview> reviews = reviewRepository
                .findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return reviews.stream()
                .map(r -> ReviewDto.ReviewResponse.from(r, authorName, false))
                .toList();
    }
}

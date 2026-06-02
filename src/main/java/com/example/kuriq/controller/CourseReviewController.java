package com.example.kuriq.controller;

import com.example.kuriq.dto.review.ReviewDto;
import com.example.kuriq.service.CourseReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "수강 후기", description = "강좌 수강 후기 & 평점 API")
@RestController
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService reviewService;

    // 평점 요약 (비로그인 가능)
    @Operation(summary = "강좌 평점 요약 조회")
    @GetMapping("/api/v1/courses/{courseId}/reviews/summary")
    public ResponseEntity<ReviewDto.SummaryResponse> getSummary(@PathVariable String courseId) {
        return ResponseEntity.ok(reviewService.getSummary(courseId));
    }

    // 리뷰 목록 (비로그인 가능)
    @Operation(summary = "리뷰 목록 조회")
    @GetMapping("/api/v1/courses/{courseId}/reviews")
    public ResponseEntity<ReviewDto.PageResponse> getReviews(
            @PathVariable String courseId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviews(courseId, userId, page, size));
    }

    // 리뷰 작성 (이수자만)
    @Operation(summary = "리뷰 작성 — 해당 강좌 이수자만 가능")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/courses/{courseId}/reviews")
    public ResponseEntity<ReviewDto.ReviewResponse> createReview(
            @AuthenticationPrincipal String userId,
            @PathVariable String courseId,
            @Valid @RequestBody ReviewDto.CreateRequest req) {
        return ResponseEntity.ok(reviewService.createReview(userId, courseId, req));
    }

    // 내 리뷰 수정
    @Operation(summary = "내 리뷰 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/v1/courses/{courseId}/reviews/me")
    public ResponseEntity<ReviewDto.ReviewResponse> updateMyReview(
            @AuthenticationPrincipal String userId,
            @PathVariable String courseId,
            @Valid @RequestBody ReviewDto.UpdateRequest req) {
        return ResponseEntity.ok(reviewService.updateMyReview(userId, courseId, req));
    }

    // 내 리뷰 삭제
    @Operation(summary = "내 리뷰 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/v1/courses/{courseId}/reviews/me")
    public ResponseEntity<Void> deleteMyReview(
            @AuthenticationPrincipal String userId,
            @PathVariable String courseId) {
        reviewService.deleteMyReview(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    // 리뷰 좋아요 토글
    // reviewId만으로 어느 강좌의 리뷰인지 서버에서 조회 가능하므로 courseId 불필요
    @Operation(summary = "리뷰 좋아요 토글")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/reviews/{reviewId}/like")
    public ResponseEntity<ReviewDto.LikeResponse> toggleLike(
            @AuthenticationPrincipal String userId,
            @PathVariable String reviewId) {
        return ResponseEntity.ok(reviewService.toggleLike(userId, reviewId));
    }
}

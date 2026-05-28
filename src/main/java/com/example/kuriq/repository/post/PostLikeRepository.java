package com.example.kuriq.repository.post;

import com.example.kuriq.entity.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, String> {

    // 좋아요 존재 여부 확인 — 토글 로직에 사용
    boolean existsByPostIdAndUserId(String postId, String userId);

    // 좋아요 단건 조회 — 취소 시 사용
    Optional<PostLike> findByPostIdAndUserId(String postId, String userId);

    // 특정 게시글의 좋아요 수 집계 — likeCount 캐시 재계산 필요 시 사용
    long countByPostId(String postId);
}
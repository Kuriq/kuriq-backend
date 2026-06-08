package com.example.kuriq.repository.post;

import com.example.kuriq.entity.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    // 전체 목록 — 최신순 (기본)
    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 전체 목록 — 조회수순, 같은 조회 수면 최신순
    Page<Post> findByIsDeletedFalseOrderByViewCountDescCreatedAtDesc(Pageable pageable);

    // 전체 목록 — 인기순(좋아요 → 조회수 → 최신순)
    Page<Post> findByIsDeletedFalseOrderByLikeCountDescViewCountDescCreatedAtDesc(Pageable pageable);

    // 단건 조회 — 삭제되지 않은 게시글만
    Optional<Post> findByIdAndIsDeletedFalse(String id);

    // 내 게시글 목록
    Page<Post> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndIsDeletedFalse(String userId);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.userId = :userId AND p.isDeleted = false")
    long sumLikeCountByUserId(@Param("userId") String userId);
}

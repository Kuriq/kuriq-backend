package com.example.kuriq.repository.post;

import com.example.kuriq.entity.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    // 전체 목록 — 최신순 (기본)
    List<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 전체 목록 — 댓글많은순, 같은 댓글 수면 최신순
    List<Post> findByIsDeletedFalseOrderByCommentCountDescCreatedAtDesc(Pageable pageable);

    // 단건 조회 — 삭제되지 않은 게시글만
    Optional<Post> findByIdAndIsDeletedFalse(String id);

    // 내 게시글 목록
    List<Post> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);
}
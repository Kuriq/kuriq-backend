package com.example.kuriq.repository.post;

import com.example.kuriq.entity.post.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, String> {

    // 게시글의 모든 댓글 + 대댓글 — 시간순 (서비스에서 최상위/대댓글 분리)
    List<PostComment> findByPostIdOrderByCreatedAtAsc(String postId);

    // 단건 조회 — 수정/삭제 작업용
    Optional<PostComment> findByIdAndIsDeletedFalse(String id);

    long countByUserIdAndIsDeletedFalse(String userId);

    Page<PostComment> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId, Pageable pageable);
}

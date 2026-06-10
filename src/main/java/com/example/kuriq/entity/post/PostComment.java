package com.example.kuriq.entity.post;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_comments",
        indexes = {
                @Index(name = "idx_post_comments_post", columnList = "postId, createdAt"),
                @Index(name = "idx_post_comments_parent", columnList = "parentId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PostComment {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 게시글 ID (posts 테이블 FK)
    @Column(nullable = false, length = 36)
    private String postId;

    // 작성자 ID (users 테이블 FK)
    @Column(nullable = false, length = 36)
    private String userId;

    // 부모 댓글 ID — 일반 댓글이면 null, 답글이면 원댓글의 id
    @Column(length = 36)
    private String parentId;

    // 댓글 내용 — 소프트 삭제 시 "삭제된 댓글입니다."로 대체
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    // true면 삭제된 상태(실제 DB에서 제거하지 않고 표시만 함)
    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 댓글 수정 — 삭제된 댓글은 수정 불가
    public void update(String content, boolean anonymous) {
        if (this.isDeleted) throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        this.content = content;
        this.anonymous = anonymous;
    }

    // 소프트 삭제 — 내용을 "삭제된 댓글입니다."로 대체
    public void softDelete() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
    }
}

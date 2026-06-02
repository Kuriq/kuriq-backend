package com.example.kuriq.entity.post;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "posts",
        indexes = {
                @Index(name = "idx_posts_user_created", columnList = "userId, createdAt"),
                @Index(name = "idx_posts_comment", columnList = "commentCount"),       // 댓글많은순 정렬용
                @Index(name = "idx_posts_active", columnList = "isDeleted, createdAt") // 활성 게시글 조회용
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 작성자 ID (users 테이블 FK)
    @Column(nullable = false, length = 36)
    private String userId;

    // 제목 (최대 50자)
    @Column(nullable = false, length = 50)
    private String title;

    // 본문 (리치 텍스트 에디터 입력값)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 조회수 캐시 — 동일 사용자/세션 1시간 내 중복 집계 제외
    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    // 좋아요 수 캐시 — post_likes 테이블과 동기화
    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    // 댓글 수 캐시 — 소프트 삭제 후에도 감소하지 않음
    @Column(nullable = false)
    @Builder.Default
    private int commentCount = 0;

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

    // 게시글 수정
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
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

    // 댓글 수 증가 — 소프트 삭제 시에도 감소하지 않으므로 증가만 있음
    public void increaseCommentCount() {
        this.commentCount++;
    }

    // 조회수 증가
    public void increaseViewCount() {
        this.viewCount++;
    }
}

package com.example.kuriq.entity.post;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_like",
                        columnNames = {"postId", "userId"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PostLike {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 게시글 ID (posts 테이블 FK)
    @Column(nullable = false, length = 36)
    private String postId;

    // 좋아요 누른 사용자 ID (users 테이블 FK)
    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }
}

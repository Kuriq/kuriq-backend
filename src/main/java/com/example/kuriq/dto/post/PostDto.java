package com.example.kuriq.dto.post;

import com.example.kuriq.entity.post.Post;
import com.example.kuriq.entity.post.PostComment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class PostDto {

    /* 요청 */

    // 게시글 작성 요청
    @Getter
    public static class CreateRequest {
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 50, message = "제목은 최대 50자까지 입력할 수 있습니다.")
        private String title;

        @NotBlank(message = "본문을 입력해주세요.")
        private String content;
    }

    // 게시글 수정 요청
    @Getter
    public static class UpdateRequest {
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 50, message = "제목은 최대 50자까지 입력할 수 있습니다.")
        private String title;

        @NotBlank(message = "본문을 입력해주세요.")
        private String content;
    }

    // 댓글 작성 요청
    @Getter
    public static class CommentCreateRequest {
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        private String content;

        // 대댓글인 경우 부모 댓글 ID (최상위 댓글이면 null)
        private String parentId;
    }

    // 댓글 수정 요청
    @Getter
    public static class CommentUpdateRequest {
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        private String content;
    }

    /* 응답 */

    // 게시글 목록 응답 (목록에서 사용)
    @Getter
    @Builder
    public static class SummaryResponse {
        private String id;
        private String title;
        private String authorName;
        private int viewCount;
        private int likeCount;
        private int commentCount;
        private LocalDateTime createdAt;

        public static SummaryResponse from(Post post, String authorName) {
            return SummaryResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .authorName(authorName)
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .createdAt(post.getCreatedAt())
                    .build();
        }
    }

    // 게시글 상세 응답
    @Getter
    @Builder
    public static class DetailResponse {
        private String id;
        private String title;
        private String content;
        private String authorId;
        private String authorName;
        private int viewCount;
        private int likeCount;
        private int commentCount;
        private boolean likedByMe;  // 로그인 사용자의 좋아요 여부
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<CommentResponse> comments;

        public static DetailResponse from(Post post, String authorName,
                                          boolean likedByMe, List<CommentResponse> comments) {
            return DetailResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .authorId(post.getUserId())
                    .authorName(authorName)
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .likedByMe(likedByMe)
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .comments(comments)
                    .build();
        }
    }

    // 댓글 응답 (대댓글 포함)
    @Getter
    @Builder
    public static class CommentResponse {
        private String id;
        private String authorId;
        private String authorName;
        private String content;
        private boolean isDeleted;
        private String parentId;
        private LocalDateTime createdAt;
        private List<CommentResponse> replies; // 대댓글 목록

        public static CommentResponse from(PostComment comment, String authorName,
                                           List<CommentResponse> replies) {
            return CommentResponse.builder()
                    .id(comment.getId())
                    .authorId(comment.getUserId())
                    .authorName(comment.isDeleted() ? null : authorName)
                    .content(comment.getContent())
                    .isDeleted(comment.isDeleted())
                    .parentId(comment.getParentId())
                    .createdAt(comment.getCreatedAt())
                    .replies(replies)
                    .build();
        }
    }

    // 좋아요 토글 응답
    @Getter
    @Builder
    public static class LikeResponse {
        private boolean liked;   // 현재 좋아요 상태
        private int likeCount;   // 변경된 좋아요 수
    }
}

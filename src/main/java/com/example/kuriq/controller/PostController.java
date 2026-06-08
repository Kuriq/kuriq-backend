package com.example.kuriq.controller;

import com.example.kuriq.dto.post.PostDto;
import com.example.kuriq.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;
import java.util.List;

@Tag(name = "게시판", description = "자유 게시판 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private static final String VIEW_SESSION_COOKIE = "community_view_session";

    // 최신순(기본) / 조회수순 / 인기순
    @Operation(summary = "게시글 목록 조회")
    @GetMapping
    public ResponseEntity<PostDto.PageResponse> getPosts(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PostDto.PageResponse result = switch (sort) {
            case "views" -> postService.getPostsByViews(page, size);
            case "popular" -> postService.getPostsByPopular(page, size);
            default -> postService.getPostsLatest(page, size);
        };
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "내 게시글 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<PostDto.PageResponse> getMyPosts(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getMyPosts(userId, page, size));
    }

    @Operation(summary = "내 댓글 목록 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/me")
    public ResponseEntity<PostDto.MyCommentPageResponse> getMyComments(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(postService.getMyComments(userId, page, size));
    }

    // 게시글 작성 (로그인 필요)
    @Operation(summary = "게시글 작성")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PostDto.SummaryResponse> createPost(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PostDto.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(userId, req));
    }

    // 게시글 상세 조회 (비로그인 가능)
    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{postId}")
    public ResponseEntity<PostDto.DetailResponse> getPost(
            @PathVariable String postId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "true") boolean increaseView,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes) { // 비로그인이면 null
        String sessionId = null;
        if (userId == null) {
            sessionId = getSessionId(httpReq);
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                setSessionCookie(httpRes, sessionId);
            }
        }

        return ResponseEntity.ok(postService.getPost(postId, userId, increaseView, sessionId));
    }

    // 게시글 수정 (본인만)
    @Operation(summary = "게시글 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{postId}")
    public ResponseEntity<PostDto.SummaryResponse> updatePost(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId,
            @Valid @RequestBody PostDto.UpdateRequest req) {
        return ResponseEntity.ok(postService.updatePost(userId, postId, req));
    }

    // 게시글 삭제 (본인만)
    @Operation(summary = "게시글 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId) {
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 토글 (로그인 필요)
    @Operation(summary = "게시글 좋아요 토글")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostDto.LikeResponse> toggleLike(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId) {
        return ResponseEntity.ok(postService.toggleLike(userId, postId));
    }

    // 댓글 작성 (로그인 필요)
    @Operation(summary = "댓글/대댓글 작성")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<PostDto.CommentResponse> createComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId,
            @Valid @RequestBody PostDto.CommentCreateRequest req) {
        return ResponseEntity.ok(postService.createComment(userId, postId, req));
    }

    // 댓글 수정 (본인만)
    @Operation(summary = "댓글 수정")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<PostDto.CommentResponse> updateComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId,
            @PathVariable String commentId,
            @Valid @RequestBody PostDto.CommentUpdateRequest req) {
        return ResponseEntity.ok(postService.updateComment(userId, commentId, req));
    }

    // 댓글 삭제 (본인만)
    @Operation(summary = "댓글 삭제")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal String userId,
            @PathVariable String postId,
            @PathVariable String commentId) {
        postService.deleteComment(userId, commentId);
        return ResponseEntity.noContent().build();
    }

    private String getSessionId(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(cookie -> VIEW_SESSION_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setSessionCookie(HttpServletResponse res, String sessionId) {
        Cookie cookie = new Cookie(VIEW_SESSION_COOKIE, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(7 * 24 * 60 * 60);
        res.addCookie(cookie);
    }
}

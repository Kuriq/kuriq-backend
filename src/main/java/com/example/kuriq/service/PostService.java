package com.example.kuriq.service;

import com.example.kuriq.dto.post.PostDto;
import com.example.kuriq.entity.post.Post;
import com.example.kuriq.entity.post.PostComment;
import com.example.kuriq.entity.post.PostLike;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.post.PostCommentRepository;
import com.example.kuriq.repository.post.PostLikeRepository;
import com.example.kuriq.repository.post.PostRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService; // ★ 추가
    private final StringRedisTemplate redisTemplate;

    // 게시글 목록 조회
    // 최신순 (기본)
    public PostDto.PageResponse getPostsLatest(int page, int size) {
        Page<Post> posts = postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        return buildPageResponse(posts);
    }

    // 조회수순 (같은 조회 수면 최신순)
    public PostDto.PageResponse getPostsByViews(int page, int size) {
        Page<Post> posts = postRepository.findByIsDeletedFalseOrderByViewCountDescCreatedAtDesc(
                PageRequest.of(page, size));
        return buildPageResponse(posts);
    }

    // 인기순 (좋아요 → 조회수 → 최신순)
    public PostDto.PageResponse getPostsByPopular(int page, int size) {
        Page<Post> posts = postRepository.findByIsDeletedFalseOrderByLikeCountDescViewCountDescCreatedAtDesc(
                PageRequest.of(page, size));
        return buildPageResponse(posts);
    }

    // 페이지 응답 빌드
    private PostDto.PageResponse buildPageResponse(Page<Post> posts) {
        List<String> userIds = posts.getContent().stream().map(Post::getUserId).distinct().toList();
        Map<String, String> nameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        List<PostDto.SummaryResponse> content = posts.getContent().stream()
                .map(p -> PostDto.SummaryResponse.from(p, nameMap.getOrDefault(p.getUserId(), "알 수 없음")))
                .toList();

        return PostDto.PageResponse.builder()
                .content(content)
                .currentPage(posts.getNumber())
                .totalPages(posts.getTotalPages())
                .totalElements(posts.getTotalElements())
                .hasNext(posts.hasNext())
                .build();
    }

    public PostDto.PageResponse getMyPosts(String userId, int page, int size) {
        Page<Post> posts = postRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return buildPageResponse(posts);
    }

    public PostDto.MyCommentPageResponse getMyComments(String userId, int page, int size) {
        Page<PostComment> commentPage = postCommentRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<String> postIds = commentPage.getContent().stream().map(PostComment::getPostId).distinct().toList();
        Map<String, Post> postMap = postRepository.findAllById(postIds).stream().collect(Collectors.toMap(Post::getId, post -> post));

        List<PostDto.MyCommentResponse> content = commentPage.getContent().stream()
                .map(comment -> {
                    Post post = postMap.get(comment.getPostId());
                    String postTitle = post == null || post.isDeleted() ? "삭제된 게시글" : post.getTitle();
                    return PostDto.MyCommentResponse.builder()
                            .id(comment.getId())
                            .postId(comment.getPostId())
                            .postTitle(postTitle)
                            .content(comment.getContent())
                            .anonymous(comment.isAnonymous())
                            .parentId(comment.getParentId())
                            .createdAt(comment.getCreatedAt())
                            .build();
                })
                .toList();

        return PostDto.MyCommentPageResponse.builder()
                .content(content)
                .currentPage(commentPage.getNumber())
                .totalPages(commentPage.getTotalPages())
                .totalElements(commentPage.getTotalElements())
                .hasNext(commentPage.hasNext())
                .build();
    }

    // 게시글 상세 조회
    @Transactional
    public PostDto.DetailResponse getPost(String postId, String userId, boolean increaseView, String sessionId) {
        Post post = getPostOrThrow(postId);

        if (increaseView && shouldIncreaseView(postId, userId, sessionId)) {
            post.increaseViewCount();
        }

        // 작성자 이름
        String authorName = userRepository.findById(post.getUserId())
                .map(User::getName).orElse("알 수 없음");

        // 본인 좋아요 여부
        boolean likedByMe = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);

        // 댓글 목록 빌드
        List<PostDto.CommentResponse> comments = buildCommentTree(postId);

        return PostDto.DetailResponse.from(post, authorName, likedByMe, comments);
    }

    private boolean shouldIncreaseView(String postId, String userId, String sessionId) {
        String viewerKey = userId != null ? "user:" + userId : sessionId != null ? "session:" + sessionId : null;
        if (viewerKey == null) return true;

        String dedupKey = "post:view:" + viewerKey + ":" + postId + ":" + LocalDate.now();
        Boolean firstView = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofHours(1));
        return !Boolean.FALSE.equals(firstView);
    }

    // 최상위 댓글 + 대댓글 조합
    private List<PostDto.CommentResponse> buildCommentTree(String postId) {
        List<PostComment> all = postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // 작성자 이름 일괄 조회
        List<String> userIds = all.stream().map(PostComment::getUserId).distinct().toList();
        Map<String, String> nameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        // 최상위 댓글만 필터 → 각 댓글의 대댓글 목록 붙이기
        return all.stream()
                .filter(c -> c.getParentId() == null)
                .map(c -> {
                    List<PostDto.CommentResponse> replies = all.stream()
                            .filter(r -> c.getId().equals(r.getParentId()))
                            .map(r -> PostDto.CommentResponse.from(r, nameMap.getOrDefault(r.getUserId(), "알 수 없음"), List.of()))
                            .toList();
                    return PostDto.CommentResponse.from(c, nameMap.getOrDefault(c.getUserId(), "알 수 없음"), replies);
                })
                .toList();
    }

    // 게시글 작성
    @Transactional
    public PostDto.SummaryResponse createPost(String userId, PostDto.CreateRequest req) {
        Post post = Post.builder()
                .userId(userId)
                .title(req.getTitle())
                .content(req.getContent())
                .anonymous(req.isAnonymous())
                .build();
        postRepository.save(post);

        // 첫 게시글 작성 뱃지 체크 (비동기)
        badgeService.checkAndAwardOnFirstPost(userId);

        String authorName = userRepository.findById(userId)
                .map(User::getName).orElse("알 수 없음");
        return PostDto.SummaryResponse.from(post, authorName);
    }

    // 게시글 수정
    @Transactional
    public PostDto.SummaryResponse updatePost(String userId, String postId, PostDto.UpdateRequest req) {
        Post post = getPostOrThrow(postId);
        validateOwner(post.getUserId(), userId); // 본인 확인

        post.update(req.getTitle(), req.getContent(), req.isAnonymous());
        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return PostDto.SummaryResponse.from(post, authorName);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(String userId, String postId) {
        Post post = getPostOrThrow(postId);
        validateOwner(post.getUserId(), userId);
        post.softDelete();
    }

    // 좋아요 토글
    @Transactional
    public PostDto.LikeResponse toggleLike(String userId, String postId) {
        Post post = getPostOrThrow(postId);

        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            // 좋아요 취소
            postLikeRepository.findByPostIdAndUserId(postId, userId)
                    .ifPresent(postLikeRepository::delete);
            post.decreaseLikeCount();
            return PostDto.LikeResponse.builder().liked(false).likeCount(post.getLikeCount()).build();
        } else {
            // 좋아요 추가
            postLikeRepository.save(PostLike.builder()
                    .postId(postId).userId(userId).build());
            post.increaseLikeCount();
            badgeService.checkAndAwardOnCommunityActivity(post.getUserId());
            return PostDto.LikeResponse.builder().liked(true).likeCount(post.getLikeCount()).build();
        }
    }

    // 댓글 작성
    @Transactional
    public PostDto.CommentResponse createComment(String userId, String postId,
                                                 PostDto.CommentCreateRequest req) {
        Post post = getPostOrThrow(postId);

        // 대댓글인 경우 부모 댓글 확인
        if (req.getParentId() != null) {
            PostComment parent = postCommentRepository.findByIdAndIsDeletedFalse(req.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));

            // 대댓글에는 다시 답글 불가 (1단계 제한)
            if (parent.getParentId() != null) {
                throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다.");
            }
        }

        PostComment comment = PostComment.builder()
                .postId(postId)
                .userId(userId)
                .parentId(req.getParentId())
                .content(req.getContent())
                .anonymous(req.isAnonymous())
                .build();
        postCommentRepository.save(comment);
        post.increaseCommentCount(); // 댓글 수 증가

        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return PostDto.CommentResponse.from(comment, authorName, List.of());
    }

    // 댓글 수정
    @Transactional
    public PostDto.CommentResponse updateComment(String userId, String commentId,
                                                 PostDto.CommentUpdateRequest req) {
        PostComment comment = postCommentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        validateOwner(comment.getUserId(), userId);

        comment.update(req.getContent(), req.isAnonymous());
        String authorName = userRepository.findById(userId).map(User::getName).orElse("알 수 없음");
        return PostDto.CommentResponse.from(comment, authorName, List.of());
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(String userId, String commentId) {
        PostComment comment = postCommentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        validateOwner(comment.getUserId(), userId);

        // 소프트 삭제 — 내용 "삭제된 댓글입니다."로 대체, commentCount 감소 없음
        comment.softDelete();
    }

    private Post getPostOrThrow(String postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    // 본인 확인 — 타인이면 403
    private void validateOwner(String ownerId, String requestUserId) {
        if (!ownerId.equals(requestUserId)) {
            throw new SecurityException("본인의 게시글/댓글만 수정·삭제할 수 있습니다.");
        }
    }
}

package com.example.kuriq.service;

import com.example.kuriq.dto.notification.request.NotificationUpdateRequest;
import com.example.kuriq.dto.notification.response.NotificationResponse;
import com.example.kuriq.dto.user.request.UserProfileUpdateRequest;
import com.example.kuriq.dto.user.response.CategoryStatsResponse;
import com.example.kuriq.dto.user.response.LearningHistoryResponse;
import com.example.kuriq.dto.user.response.SocialAccountResponse;
import com.example.kuriq.dto.user.response.UserStatsResponse;
import com.example.kuriq.entity.notification.NotificationSetting;
import com.example.kuriq.entity.notification.UnsubscribeToken;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.LearningHistory;
import com.example.kuriq.entity.user.SocialAccount;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.post.PostCommentRepository;
import com.example.kuriq.repository.post.PostRepository;
import com.example.kuriq.repository.notification.UnsubscribeTokenRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import com.example.kuriq.repository.roadmap.LearningHistoryRepository;
import com.example.kuriq.repository.roadmap.RoadmapRepository;
import com.example.kuriq.repository.user.SocialAccountRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UnsubscribeTokenRepository unsubscribeTokenRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final CourseRepository courseRepository;
    private final RoadmapRepository roadmapRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;

    // 프로필 조회
    public User getUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    @Transactional
    public User updateProfile(String userId, UserProfileUpdateRequest req) {
        User user = getUser(userId);
        user.updateProfile(req.getName(), req.getProfileIcon(), req.getProfileColor());
        return user;
    }

    // 소셜 계정 목록 조회
    public List<SocialAccountResponse> getSocialAccounts(String userId) {
        return socialAccountRepository.findByUserId(userId).stream()
                .map(SocialAccountResponse::from)
                .collect(Collectors.toList());
    }

    // 소셜 계정 연동 해제
    @Transactional
    public void unlinkSocialAccount(String userId, String providerStr) {
        SocialAccount.Provider provider =
                SocialAccount.Provider.valueOf(providerStr.toUpperCase());
        socialAccountRepository.findByUserId(userId).stream()
                .filter(sa -> sa.getProvider() == provider)
                .findFirst()
                .ifPresent(socialAccountRepository::delete);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteAccount(String userId, String password) {
        User user = getUser(userId);

        if (user.getAuthProvider() == User.AuthProvider.LOCAL) {
            if (password == null || password.isBlank()) {
                throw new IllegalArgumentException("비밀번호를 입력해 주세요");
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new IllegalArgumentException("비밀번호가 올바르지 않습니다");
            }
        }

        // 소셜 계정 연동 정보 삭제 (탈퇴 후 재가입 시 동일 소셜 계정으로 신규 가입 가능하게)
        socialAccountRepository.deleteByUserId(userId);

        user.softDelete();
    }

    // 알림 설정 조회
    public NotificationResponse getNotificationSettings(String userId) {
        NotificationSetting ns = notificationSettingRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));
        return NotificationResponse.from(ns);
    }

    // 알림 설정 수정
    @Transactional
    public NotificationResponse updateNotificationSettings(String userId,
                                                           NotificationUpdateRequest req) {
        NotificationSetting ns = notificationSettingRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));
        ns.update(
                req.getEmailEnabled(),
                req.getKakaoEnabled(),
                req.getLearningDay(),
                req.getLearningTime(),
                req.getWeeklyStartAlert(),
                req.getIncompleteReminder(),
                req.getInactivityAlert(),
                req.getCompletionAlert()
        );
        return NotificationResponse.from(ns);
    }

    // 알림 수신 거부 (이메일 링크의 token으로 채널 비활성화)
    // 수신 거부 링크의 토큰으로 사용자와 채널을 찾아 해당 알림을 끄고, 토큰을 1회용으로 처리하는 서비스 로직
    @Transactional
    public void unsubscribeNotification(String token) {

        // token으로 어떤 사용자인지, 어떤 채널인지 확인
        UnsubscribeToken unsubscribeToken = unsubscribeTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 수신 거부 토큰입니다"));

        // 해당 사용자의 알림 설정 조회(사용자 알림 설정 엔티티 가져옴)
        NotificationSetting ns = notificationSettingRepository.findById(unsubscribeToken.getUserId())
                .orElseThrow(() -> new RuntimeException("알림 설정을 찾을 수 없습니다"));

        // 해당 채널만 off
        if (unsubscribeToken.getChannel() == UnsubscribeToken.Channel.EMAIL) {
            ns.update(false, ns.getKakaoEnabled(),
                    ns.getLearningDay(), ns.getLearningTime(),
                    ns.getWeeklyStartAlert(), ns.getIncompleteReminder(),
                    ns.getInactivityAlert(), ns.getCompletionAlert());
        } else {
            ns.update(ns.getEmailEnabled(), false,
                    ns.getLearningDay(), ns.getLearningTime(),
                    ns.getWeeklyStartAlert(), ns.getIncompleteReminder(),
                    ns.getInactivityAlert(), ns.getCompletionAlert());
        }

        // 사용한 토큰 삭제 (1회용)
        unsubscribeTokenRepository.delete(unsubscribeToken);
    }

    // 학습 통계
    public UserStatsResponse getStats(String userId) {

        // 이수 강좌 총 개수
        // learning_history 테이블 COUNT
        long totalCourses = learningHistoryRepository.countByUserId(userId);

        // 총 학습 시간
        // 이수한 강좌들의 estimated_hours 합산
        BigDecimal totalHours = learningHistoryRepository.sumEstimatedHoursByUserId(userId);

        // 연속 학습 일수 계산
        // completedAt 날짜 기준으로 오늘부터 카운트
        List<LearningHistory> histories = learningHistoryRepository
                .findByUserIdOrderByCompletedAtDesc(userId);

        int streakDays = 0;
        if (!histories.isEmpty()) {

            // completedAt에서 날짜만 추출해서 Set으로 만듦 (중복 제거)
            Set<LocalDate> datesWithActivity = histories.stream()
                    .map(h -> h.getCompletedAt().toLocalDate())
                    .collect(Collectors.toSet());

            LocalDate check = LocalDate.now();

            // 오늘 활동이 없으면 어제부터 체크 시작
            if (!datesWithActivity.contains(check)) {
                check = check.minusDays(1);
            }

            // 날짜가 연속으로 존재하는 동안 하루씩 올라가며 카운트
            while (datesWithActivity.contains(check)) {
                streakDays++;
                check = check.minusDays(1);
            }
        }

        // 완료 로드맵 수
        // roadmaps 테이블에서 is_completed = true 카운트
        long completedRoadmaps = roadmapRepository.countByUserIdAndIsCompletedTrue(userId);

        long totalCommunityPosts = postRepository.countByUserIdAndIsDeletedFalse(userId);
        long totalCommunityComments = postCommentRepository.countByUserIdAndIsDeletedFalse(userId);

        return UserStatsResponse.builder()
                .totalCompletedCourses(totalCourses)
                .totalLearningHours(totalHours)
                .streakDays(streakDays)
                .completedRoadmapCount(completedRoadmaps)
                .totalCommunityPosts(totalCommunityPosts)
                .totalCommunityComments(totalCommunityComments)
                .build();
    }

    // 분야별 학습 현황
    public List<CategoryStatsResponse> getCategoryStats(String userId) {

        // 카테고리별 이수 강좌 수 조회
        // 이수 수 내림차순 정렬
        List<Object[]> rows = learningHistoryRepository.countByCategoryForUser(userId);

        // 이력 없으면 빈 리스트 반환
        if (rows.isEmpty()) return List.of();

        // 가장 많이 이수한 카테고리 수를 기준으로 상대 진행률 계산
        // 이미 내림차순 정렬이라 첫 번째 값이 최대값
        long maxCount = ((Number) rows.get(0)[1]).longValue();

        return rows.stream().map(row -> {
            String category = (String) row[0];
            long count      = ((Number) row[1]).longValue();

            // 상대 진행률 계산 (소수점 1자리)
            double percent = maxCount == 0 ? 0 : (double) count / maxCount * 100;

            return CategoryStatsResponse.builder()
                    .category(category)
                    .completedCount(count)
                    .progressPercent(Math.round(percent * 10.0) / 10.0)
                    .build();
        }).toList();
    }

    // 학습 이력 조회
    public List<LearningHistoryResponse> getLearningHistory(String userId, int page, int size) {

        // 최신순 페이징 조회
        Pageable pageable = PageRequest.of(page, size);
        List<LearningHistory> histories = learningHistoryRepository
                .findByUserIdOrderByCompletedAtDesc(userId, pageable);

        // 이력 없으면 빈 리스트 반환
        if (histories.isEmpty()) return List.of();

        // courseId 목록 추출
        // distinct로 중복 제거 후 배치 조회 → N+1 방지
        List<String> courseIds = histories.stream()
                .map(LearningHistory::getCourseId)
                .distinct()
                .toList();

        // courseId → Course Map으로 변환
        // DTO 변환 시 Map에서 바로 꺼내 씀
        Map<String, Course> courseMap = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        // 이력 DTO 변환
        // 강좌가 삭제된 경우 기본값으로 대체
        return histories.stream().map(h -> {
            Course course   = courseMap.get(h.getCourseId());
            String title    = course != null ? course.getTitle()    : "삭제된 강좌";
            String platform = course != null ? course.getPlatform().name() : "-";
            String category = course != null ? course.getCategory() : "-";
            return LearningHistoryResponse.from(h, title, platform, category);
        }).toList();
    }
}

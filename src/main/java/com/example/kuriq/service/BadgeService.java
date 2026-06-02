package com.example.kuriq.service;

import com.example.kuriq.dto.badge.BadgeResponse;
import com.example.kuriq.entity.badge.Badge;
import com.example.kuriq.entity.badge.BadgeType;
import com.example.kuriq.repository.badge.BadgeRepository;
import com.example.kuriq.repository.roadmap.LearningHistoryRepository;
import com.example.kuriq.repository.roadmap.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final ZoneId UTC = ZoneId.of("UTC");

    // QURI_MASTER 달성 조건 — 이 Set 의 뱃지를 전부 보유해야 부여됨.
    private static final Set<BadgeType> REQUIRED_FOR_MASTER = Set.of(
            BadgeType.SEEDLING,
            BadgeType.STREAK_3,  BadgeType.STREAK_7,
            BadgeType.STREAK_30, BadgeType.STREAK_100,
            BadgeType.COURSE_5,  BadgeType.COURSE_10, BadgeType.COURSE_30,
            BadgeType.ROADMAP_1, BadgeType.ROADMAP_3,
            BadgeType.FIRST_POST
    );

    private final BadgeRepository badgeRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final RoadmapRepository roadmapRepository;

    //  내 뱃지 목록 조회
    @Transactional(readOnly = true)
    public List<BadgeResponse> getMyBadges(String userId) {
        List<Badge> acquired = badgeRepository.findByUserIdOrderByAcquiredAtDesc(userId);
        return BadgeResponse.buildFullList(acquired);
    }

    //  트리거 1: 강좌 완료
    //  체크 대상: SEEDLING, COURSE_5/10/30, STREAK_3/7/30/100
    @Async
    @Transactional
    public void checkAndAwardOnCourseComplete(String userId) {
        List<BadgeType> awarded = new ArrayList<>();

        // 누적 강좌 수 기반
        long totalCompleted = learningHistoryRepository.countByUserId(userId);

        if (totalCompleted >= 1)  award(userId, BadgeType.SEEDLING,   awarded);
        if (totalCompleted >= 5)  award(userId, BadgeType.COURSE_5,   awarded);
        if (totalCompleted >= 10) award(userId, BadgeType.COURSE_10,  awarded);
        if (totalCompleted >= 30) award(userId, BadgeType.COURSE_30,  awarded);

        // 연속 학습 스트릭 (KST 자정 기준)
        int streak = calculateStreak(userId);

        if (streak >= 3)   award(userId, BadgeType.STREAK_3,   awarded);
        if (streak >= 7)   award(userId, BadgeType.STREAK_7,   awarded);
        if (streak >= 30)  award(userId, BadgeType.STREAK_30,  awarded);
        if (streak >= 100) award(userId, BadgeType.STREAK_100, awarded);

        if (!awarded.isEmpty()) {
            checkQuriMaster(userId);
        }
    }

    //  트리거 2: 로드맵 전체 완료
    //  체크 대상: ROADMAP_1, ROADMAP_3
    @Async
    @Transactional
    public void checkAndAwardOnRoadmapComplete(String userId) {
        List<BadgeType> awarded = new ArrayList<>();

        long completedRoadmaps = roadmapRepository.countByUserIdAndIsCompletedTrue(userId);

        if (completedRoadmaps >= 1) award(userId, BadgeType.ROADMAP_1, awarded);
        if (completedRoadmaps >= 3) award(userId, BadgeType.ROADMAP_3, awarded);

        if (!awarded.isEmpty()) {
            checkQuriMaster(userId);
        }
    }

    //  트리거 3: 게시글 최초 작성
    //  체크 대상: FIRST_POST
    @Async
    @Transactional
    public void checkAndAwardOnFirstPost(String userId) {
        List<BadgeType> awarded = new ArrayList<>();
        award(userId, BadgeType.FIRST_POST, awarded);
        if (!awarded.isEmpty()) {
            checkQuriMaster(userId);
        }
    }

    // 뱃지 1개 부여 시도
    // 이미 보유 중이면 skip
    private void award(String userId, BadgeType type, List<BadgeType> awarded) {
        if (badgeRepository.existsByUserIdAndBadgeType(userId, type)) {
            return;
        }
        try {
            badgeRepository.save(Badge.of(userId, type));
            awarded.add(type);
            log.info("뱃지 부여: userId={}, badge={}", userId, type);
        } catch (DataIntegrityViolationException e) {
            log.debug("뱃지 UK 충돌 무시: userId={}, badge={}", userId, type);
        }
    }

    // QURI_MASTER 조건 확인
    // REQUIRED_FOR_MASTER 의 뱃지를 전부 보유하면 QURI_MASTER 부여
    private void checkQuriMaster(String userId) {
        if (badgeRepository.existsByUserIdAndBadgeType(userId, BadgeType.QURI_MASTER)) {
            return;
        }
        Set<BadgeType> owned = new HashSet<>(badgeRepository.findBadgeTypesByUserId(userId));
        if (owned.containsAll(REQUIRED_FOR_MASTER)) {
            try {
                badgeRepository.save(Badge.of(userId, BadgeType.QURI_MASTER));
                log.info("QURI_MASTER 달성: userId={}", userId);
            } catch (DataIntegrityViolationException e) {
                log.debug("QURI_MASTER UK 충돌 무시: userId={}", userId);
            }
        }
    }

    // 연속 학습 스트릭 계산

    /**
     * learning_history.completed_at 을 KST LocalDate 로 변환 후
     * 오늘(또는 어제)부터 역방향으로 연속된 날 수를 카운트한다.
     *
     * - 오늘 완료한 강좌가 있으면 오늘부터 역산
     * - 오늘 완료 강좌가 없고 어제 있으면 어제부터 역산 (당일 미학습 허용)
     * - 그 외 경우(마지막 학습이 2일 이상 전) 스트릭 = 0
     */
    private int calculateStreak(String userId) {
        List<LocalDateTime> completedDateTimes =
                learningHistoryRepository.findCompletedAtByUserIdOrderByDesc(userId);

        if (completedDateTimes.isEmpty()) return 0;

        // KST 저장 기준 LocalDate 변환 후 중복 제거, 내림차순 정렬
        List<LocalDate> dates = completedDateTimes.stream()
                .map(LocalDateTime::toLocalDate)// DB가 KST 저장이므로 그냥 toLocalDate()
                .distinct()
                .sorted((a, b) -> b.compareTo(a))
                .toList();

        LocalDate today = LocalDate.now(KST);
        LocalDate mostRecent = dates.get(0);

        // 마지막 학습이 오늘도 어제도 아니면 스트릭 없음
        if (!mostRecent.equals(today) && !mostRecent.equals(today.minusDays(1))) {
            return 0;
        }

        // 가장 최근 날짜부터 역산하며 연속 일수 카운트
        int streak = 0;
        LocalDate expected = mostRecent;

        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }
}

package com.example.kuriq.service;

import com.example.kuriq.entity.notification.NotificationSetting;
import com.example.kuriq.entity.roadmap.Roadmap;
import com.example.kuriq.entity.roadmap.RoadmapItem;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.roadmap.RoadmapItemRepository;
import com.example.kuriq.repository.roadmap.RoadmapRepository;
import com.example.kuriq.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationScheduler {

    private final NotificationSettingRepository notificationSettingRepository;
    private final RoadmapRepository roadmapRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final String DASHBOARD_URL = "https://kuriq.com/dashboard";

    // 주간 시작 알림 (매 분마다 체크 - TODO: 테스트용, 운영 시 0 0 * * * * 으로 변경)
    @Scheduled(cron = "0 * * * * *")
    public void sendWeeklyStartAlert() {
        NotificationSetting.DayOfWeek today = convertDayOfWeek(LocalDate.now().getDayOfWeek());
        int currentHour = LocalDateTime.now().getHour();

        List<NotificationSetting> targets = notificationSettingRepository
                .findWeeklyStartTargets(today, currentHour);

        for (NotificationSetting ns : targets) {
            // 활성 로드맵 있는지 확인
            Roadmap activeRoadmap = roadmapRepository
                    .findByUserIdAndIsActiveTrue(ns.getUserId()).orElse(null);
            if (activeRoadmap == null) continue;

            // 이번 주차 첫 번째 미완료 강좌 이름
            int currentWeek = getCurrentWeekNumber(activeRoadmap);
            List<RoadmapItem> items = roadmapItemRepository
                    .findByRoadmapIdAndWeekNumberOrderByOrderInWeekAsc(
                            activeRoadmap.getId(), currentWeek);
            String courseName = items.stream()
                    .filter(i -> !i.getIsCompleted())
                    .findFirst()
                    .map(i -> i.getCourse().getTitle())
                    .orElse("첫 번째 강좌");

            User user = userRepository.findById(ns.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null) continue;

            try {
                emailService.sendWeeklyStartEmail(
                        user.getEmail(), user.getId(), user.getName(), courseName, DASHBOARD_URL);
                log.info("주간 시작 알림 발송: userId={}", ns.getUserId());
            } catch (Exception e) {
                log.error("주간 시작 알림 발송 실패: userId={}, error={}", ns.getUserId(), e.getMessage());
            }
        }
    }

    // 미완료 리마인드 (매일 10:00)
    @Scheduled(cron = "0 0 10 * * *")
    public void sendIncompleteReminder() {
        List<NotificationSetting> targets = notificationSettingRepository
                .findIncompleteReminderTargets();

        for (NotificationSetting ns : targets) {
            Roadmap activeRoadmap = roadmapRepository
                    .findByUserIdAndIsActiveTrue(ns.getUserId()).orElse(null);
            if (activeRoadmap == null) continue;

            int currentWeek = getCurrentWeekNumber(activeRoadmap);
            long incompleteCount = roadmapItemRepository
                    .countByRoadmapIdAndWeekNumberAndIsCompletedFalse(
                            activeRoadmap.getId(), currentWeek);

            if (incompleteCount == 0) continue;

            User user = userRepository.findById(ns.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null) continue;

            try {
                emailService.sendIncompleteReminderEmail(
                        user.getEmail(), user.getId(), user.getName(), (int) incompleteCount, DASHBOARD_URL);
                log.info("미완료 리마인드 발송: userId={}", ns.getUserId());
            } catch (Exception e) {
                log.error("미완료 리마인드 발송 실패: userId={}, error={}", ns.getUserId(), e.getMessage());
            }
        }
    }

    // 장기 미활동 알림 (매일 10:00)
    @Scheduled(cron = "0 0 10 * * *")
    public void sendInactivityAlert() {
        List<NotificationSetting> targets = notificationSettingRepository
                .findInactivityAlertTargets();

        for (NotificationSetting ns : targets) {
            User user = userRepository.findById(ns.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null) continue;

            long daysSinceLastActivity = ChronoUnit.DAYS.between(
                    user.getUpdatedAt().toLocalDate(), LocalDate.now());

            // 7일 또는 14일 경과 시에만 발송
            if (daysSinceLastActivity != 7 && daysSinceLastActivity != 14) continue;

            try {
                emailService.sendInactivityEmail(
                        user.getEmail(), user.getId(), user.getName(), DASHBOARD_URL);
                log.info("장기 미활동 알림 발송: userId={}, days={}", ns.getUserId(), daysSinceLastActivity);
            } catch (Exception e) {
                log.error("장기 미활동 알림 발송 실패: userId={}, error={}", ns.getUserId(), e.getMessage());
            }
        }
    }

    // 로드맵 시작일 기준으로 현재 주차 계산(클래스 내부에서만 사용하는 메서드)
    private int getCurrentWeekNumber(Roadmap roadmap) {
        if (roadmap.getActivatedAt() == null) return 1;
        long days = ChronoUnit.DAYS.between(
                roadmap.getActivatedAt().toLocalDate(), LocalDate.now());
        return (int) (days / 7) + 1;
    }

    // Java 표준 DayOfWeek(MONDAY~SUNDAY)를 우리 enum(MON~SUN)으로 변환
    private NotificationSetting.DayOfWeek convertDayOfWeek(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> NotificationSetting.DayOfWeek.MON;
            case TUESDAY -> NotificationSetting.DayOfWeek.TUE;
            case WEDNESDAY -> NotificationSetting.DayOfWeek.WED;
            case THURSDAY -> NotificationSetting.DayOfWeek.THU;
            case FRIDAY -> NotificationSetting.DayOfWeek.FRI;
            case SATURDAY -> NotificationSetting.DayOfWeek.SAT;
            case SUNDAY -> NotificationSetting.DayOfWeek.SUN;
        };
    }
}

package com.example.kuriq.repository.notification;

import com.example.kuriq.entity.notification.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// PK = userId (users와 1:1). findById(userId)로 바로 조회.
// 회원가입 시 NotificationSetting.createDefault(userId)로 자동 생성.
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, String> {

    // 주간 시작 알림 대상 조회
    @Query("SELECT ns FROM NotificationSetting ns " +
            "WHERE ns.emailEnabled = true AND ns.weeklyStartAlert = true " +
            "AND ns.learningDay = :dayOfWeek " +
            "AND HOUR(ns.learningTime) = :hour")
    List<NotificationSetting> findWeeklyStartTargets(
            @Param("dayOfWeek") NotificationSetting.DayOfWeek dayOfWeek,
            @Param("hour") int hour);


    // 미완료 리마인드 대상 조회
    @Query("SELECT ns FROM NotificationSetting ns " +
            "WHERE ns.emailEnabled = true AND ns.incompleteReminder = true")
    List<NotificationSetting> findIncompleteReminderTargets();

    // 장기 미활동 알림 대상 조회
    @Query("SELECT ns FROM NotificationSetting ns " +
            "WHERE ns.emailEnabled = true AND ns.inactivityAlert = true")
    List<NotificationSetting> findInactivityAlertTargets();

    // 완료 축하 알림 대상 조회
    @Query("SELECT ns FROM NotificationSetting ns " +
            "WHERE ns.emailEnabled = true AND ns.completionAlert = true " +
            "AND ns.userId = :userId")
    Optional<NotificationSetting> findCompletionAlertTarget(@Param("userId") String userId);
}


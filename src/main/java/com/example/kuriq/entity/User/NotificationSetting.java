package com.example.kuriq.entity.user;

// users 1:1 notification_settings — user_id가 PK이자 FK.
// 알림 채널(이메일/카카오), 발송 요일/시간, 유형별 On/Off를 관리한다.

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {
    /** users.id와 동일 (1:1) */
    @Id
    @Column(length = 36)
    private String userId;

    @Column(nullable = false)
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    private Boolean kakaoEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private DayOfWeek learningDay = DayOfWeek.MON;

    @Column(nullable = false)
    private LocalTime learningTime = LocalTime.of(9, 0);

    /** 유형별 알림 On/Off */
    @Column(nullable = false)
    private Boolean weeklyStartAlert = true;

    @Column(nullable = false)
    private Boolean incompleteReminder = true;

    @Column(nullable = false)
    private Boolean inactivityAlert = true;

    @Column(nullable = false)
    private Boolean completionAlert = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void preUpdate() { updatedAt = LocalDateTime.now(); }
    public enum DayOfWeek { MON, TUE, WED, THU, FRI, SAT, SUN }

    /** 회원가입 시 기본 설정 생성 */
    public static NotificationSetting createDefault(String userId) {
        NotificationSetting ns = new NotificationSetting();
        ns.userId = userId;
        return ns;
    }

    public void update(Boolean emailEnabled, Boolean kakaoEnabled,
                       DayOfWeek learningDay, LocalTime learningTime,
                       Boolean weeklyStart, Boolean incomplete,
                       Boolean inactivity, Boolean completion) {
        this.emailEnabled = emailEnabled;
        this.kakaoEnabled = kakaoEnabled;
        this.learningDay = learningDay;
        this.learningTime = learningTime;
        this.weeklyStartAlert = weeklyStart;
        this.incompleteReminder = incomplete;
        this.inactivityAlert = inactivity;
        this.completionAlert = completion;
    }
}


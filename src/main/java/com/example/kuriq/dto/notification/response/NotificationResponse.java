package com.example.kuriq.dto.notification.response;

import com.example.kuriq.entity.notification.NotificationSetting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

/**
 * 사용자 알림 설정을 클라이언트에 반환하기 위한 응답 DTO.
 * NotificationSetting 엔티티를 기반으로 생성된다.
 * 알림 설정 조회(GET), 수정(PUT) 공통 응답 DTO => 응답 형태가 같아서
 */

@Getter
@Builder
public class NotificationResponse {
    private Boolean emailEnabled;
    private Boolean kakaoEnabled;
    private NotificationSetting.DayOfWeek learningDay;
    private LocalTime learningTime;
    private Boolean weeklyStartAlert;
    private Boolean incompleteReminder;
    private Boolean inactivityAlert;
    private Boolean completionAlert;

    // from()로 Entity → DTO 변환 메서드
    public static NotificationResponse from(NotificationSetting ns) {
        // ns에 있는 값들을 꺼내서 DTO에 넣음
        return NotificationResponse.builder()
                .emailEnabled(ns.getEmailEnabled())
                .kakaoEnabled(ns.getKakaoEnabled())
                .learningDay(ns.getLearningDay())
                .learningTime(ns.getLearningTime())
                .weeklyStartAlert(ns.getWeeklyStartAlert())
                .incompleteReminder(ns.getIncompleteReminder())
                .inactivityAlert(ns.getInactivityAlert())
                .completionAlert(ns.getCompletionAlert())
                .build();
    }
}

package com.example.kuriq.dto.notification.request;

import com.example.kuriq.entity.notification.NotificationSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalTime;

/**
 * 알림 설정 수정 요청 DTO
 */
@Getter
@Schema(description = "알림 설정 수정 요청")
public class NotificationUpdateRequest {

    @Schema(description = "이메일 알림 활성화", example = "true")
    private Boolean emailEnabled;

    @Schema(description = "카카오 알림 활성화", example = "false")
    private Boolean kakaoEnabled;

    @Schema(description = "학습 시작 요일", example = "MON")
    private NotificationSetting.DayOfWeek learningDay;  // 알림 받을 요일

    @Schema(description = "학습 시작 시간", example = "09:00:00")
    private LocalTime learningTime;  // 알림 받을 시간

    @Schema(description = "주간 학습 시작 알림", example = "true")
    private Boolean weeklyStartAlert;

    @Schema(description = "미완료 강의 리마인드 알림", example = "true")
    private Boolean incompleteReminder;

    @Schema(description = "장기 미활동 알림", example = "true")
    private Boolean inactivityAlert;

    @Schema(description = "완료 축하 알림", example = "true")
    private Boolean completionAlert;
}

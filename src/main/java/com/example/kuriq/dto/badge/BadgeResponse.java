package com.example.kuriq.dto.badge;

import com.example.kuriq.entity.badge.Badge;
import com.example.kuriq.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BadgeResponse {

    // 뱃지 ID (미획득이면 null)
    private String id;

    // 뱃지 ENUM 키
    private String badgeType;

    // 뱃지 표시 이름
    private String displayName;

    // 뱃지 달성 메시지
    private String description;

    // 획득 여부
    private boolean acquired;

    // 획득 시각 (미획득이면 null)
    private LocalDateTime acquiredAt;

    // 진행 수치 (없으면 null)
    private Integer progressCurrent;

    // 진행 목표 (없으면 null)
    private Integer progressTotal;

    // 변환 메서드

    // 획득한 Badge 엔티티 -> 응답 DTO
    public static BadgeResponse from(Badge badge, Integer progressCurrent, Integer progressTotal) {
        BadgeType type = badge.getBadgeType();
        return BadgeResponse.builder()
                .id(badge.getId())
                .badgeType(type.name())
                .displayName(type.getDisplayName())
                .description(type.getDescription())
                .acquired(true)
                .acquiredAt(badge.getAcquiredAt())
                .progressCurrent(progressCurrent)
                .progressTotal(progressTotal)
                .build();
    }

    // 미획득 뱃지 타입 -> 잠금 상태 DTO
    public static BadgeResponse locked(BadgeType type, Integer progressCurrent, Integer progressTotal) {
        return BadgeResponse.builder()
                .id(null)
                .badgeType(type.name())
                .displayName(type.getDisplayName())
                .description(type.getDescription())
                .acquired(false)
                .acquiredAt(null)
                .progressCurrent(progressCurrent)
                .progressTotal(progressTotal)
                .build();
    }
}

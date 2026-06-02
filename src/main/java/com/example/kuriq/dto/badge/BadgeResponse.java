package com.example.kuriq.dto.badge;

import com.example.kuriq.entity.badge.Badge;
import com.example.kuriq.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // 변환 메서드

    // 획득한 Badge 엔티티 -> 응답 DTO
    public static BadgeResponse from(Badge badge) {
        BadgeType type = badge.getBadgeType();
        return BadgeResponse.builder()
                .id(badge.getId())
                .badgeType(type.name())
                .displayName(type.getDisplayName())
                .description(type.getDescription())
                .acquired(true)
                .acquiredAt(badge.getAcquiredAt())
                .build();
    }

    // 미획득 뱃지 타입 -> 잠금 상태 DTO
    private static BadgeResponse locked(BadgeType type) {
        return BadgeResponse.builder()
                .id(null)
                .badgeType(type.name())
                .displayName(type.getDisplayName())
                .description(type.getDescription())
                .acquired(false)
                .acquiredAt(null)
                .build();
    }

    // 전체 뱃지 목록 응답 (획득 + 미획득 모두 포함)
    public static List<BadgeResponse> buildFullList(List<Badge> acquiredBadges) {
        Map<BadgeType, Badge> badgeMap = acquiredBadges.stream()
                .collect(Collectors.toMap(Badge::getBadgeType, badge -> badge));

        return Arrays.stream(BadgeType.values())
                .map(type -> {
                    Badge badge = badgeMap.get(type);
                    return badge != null ? from(badge) : locked(type);
                })
                .toList();
    }
}

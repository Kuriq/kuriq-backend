package com.example.kuriq.entity.badge;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "badges",
        indexes = {
                @Index(name = "idx_badges_user_acquired", columnList = "user_id, acquired_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_badge_user_type", columnNames = {"user_id", "badge_type"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 뱃지 소유 사용자 ID
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    // 뱃지 종류
    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false, length = 30)
    private BadgeType badgeType;

    // 뱃지 획득 시각
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    @PrePersist
    private void prePersist() {
        acquiredAt = LocalDateTime.now();
    }

    // 팩토리 메서드
    public static Badge of(String userId, BadgeType badgeType) {
        Badge badge = new Badge();
        badge.userId = userId;
        badge.badgeType = badgeType;
        return badge;
    }
}

package com.example.kuriq.entity.space;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "study_spaces",
        indexes = {
                @Index(name = "idx_spaces_location", columnList = "latitude, longitude"),
                @Index(name = "idx_spaces_type", columnList = "type"),
                @Index(name = "idx_spaces_active", columnList = "isActive"), // TODO: DB 컬럼명으로 교체 고려
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudySpace {

    // 공간 ID
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 공간 이름
    @Column(nullable = false)
    private String name;

    // 공간 유형
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType type;

    // 주소
    @Column(nullable = false, length = 500)
    private String address;

    // 위도
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    // 경도
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    // 운영 시간
    @Column(length = 500)
    private String operatingHours;

    // 전화번호
    @Column(length = 20)
    private String phone;

    // 와이파이 제공 여부
    private Boolean hasWifi = false;

    // 콘센트 제공 여부
    private Boolean hasPowerOutlet = false;

    // 활성화 여부(false면 검색 결과에서 제외)
    private Boolean is_active = true;

    // 데이터 마지막 갱신 시각
    private LocalDateTime lastUpdatedAt;

    // DB insert 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum SpaceType {
        LIBRARY,
        LIFELONG_LEARNING,
        FIFTY_PLUS,
        YOUTH_CENTER,
        CAFE
    }
}

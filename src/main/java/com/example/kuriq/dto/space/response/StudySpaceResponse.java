package com.example.kuriq.dto.space.response;

import com.example.kuriq.entity.space.StudySpace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "학습 공간 조회 응답")
public class StudySpaceResponse {

    @Schema(description = "공간 ID", example = "uuid-1234")
    private String id;

    @Schema(description = "공간 이름", example = "강남구립 논현도서관")
    private String name;

    // LIBRARY / LIFELONG_LEARNING / FIFTY_PLUS / CAFE 중 하나
    @Schema(description = "공간 유형", example = "LIBRARY")
    private String type;

    @Schema(description = "주소", example = "서울 강남구 학동로 326")
    private String address;

    @Schema(description = "위도", example = "37.5172")
    private java.math.BigDecimal latitude;

    @Schema(description = "경도", example = "127.0473")
    private java.math.BigDecimal longitude;

    @Schema(description = "운영 시간", example = "평일 09:00~18:00 / 주말 휴무")
    private String operatingHours;

    @Schema(description = "전화번호", example = "02-1234-5678")
    private String phone;

    @Schema(description = "와이파이 여부", example = "true")
    private Boolean hasWifi;

    @Schema(description = "콘센트 여부", example = "false")
    private Boolean hasPowerOutlet;

    // 요청 위치 기준 거리(미터), 소수점 없이 정수로 반환
    @Schema(description = "현재 위치 기준 거리(m)", example = "450")
    private Integer distanceMeters;

    // Entity → DTO 변환 메서드
    // 거리값을 응답에 포함시키기 위해 distanceMeters 추가
    // distanceMeters는 Service에서 Haversine으로 계산한 값을 넘겨받음
    public static StudySpaceResponse from(StudySpace space, double distanceMeters) {
        return StudySpaceResponse.builder()
                .id(space.getId())
                .name(space.getName())
                .type(space.getType().name())
                .address(space.getAddress())
                .latitude(space.getLatitude())
                .longitude(space.getLongitude())
                .operatingHours(space.getOperatingHours())
                .phone(space.getPhone())
                .hasWifi(space.getHasWifi())
                .hasPowerOutlet(space.getHasPowerOutlet())
                .distanceMeters((int) Math.round(distanceMeters))
                .build();
    }
}

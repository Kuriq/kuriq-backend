package com.example.kuriq.service;

import com.example.kuriq.client.KakaoLocalClient;
import com.example.kuriq.dto.space.response.StudySpaceResponse;
import com.example.kuriq.entity.space.StudySpace;
import com.example.kuriq.repository.space.StudySpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이라 readOnly → 성능 최적화
public class StudySpaceService {

    private final StudySpaceRepository studySpaceRepository;
    private final KakaoLocalClient kakaoLocalClient;

    // 반경 기본값: 2000m / 최대값: 10000m
    private static final int DEFAULT_RADIUS = 2000;
    private static final int MAX_RADIUS     = 10000;

    private static final int MAX_RESULT_COUNT = 20;
    private static final int MIN_PRIVATE_SPACE_COUNT = 4;

    // 위치 기반 주변 학습 공간 조회
    // 처리 순서:
    //   1) 공공 공간(도서관, 평생학습관, 50플러스) 먼저 조회
    //   2) 남는 슬롯만큼 카카오 로컬 API에서 민간 공간(카페/스터디카페) 조회
    //   3) 공공 공간을 우선 반환하고, 민간 공간은 후순위로 최대 20개까지 반환
    public List<StudySpaceResponse> getNearbySpaces(double lat, double lng, Integer radius, StudySpace.SpaceType type) {

        // 반경 유효성 처리: null이면 기본값, MAX_RADIUS 초과 시 최대값으로 고정
        int effectiveRadius = (radius == null) ? DEFAULT_RADIUS : Math.min(radius, MAX_RADIUS);

        if (type == StudySpace.SpaceType.CAFE) {
            return getNearbyCafeSpaces(lat, lng, effectiveRadius);
        }

        if (type != null) {
            return getStoredSpacesByType(lat, lng, effectiveRadius, type);
        }

        return getMixedSpaces(lat, lng, effectiveRadius);
    }

    private List<StudySpaceResponse> getMixedSpaces(double lat, double lng, int effectiveRadius) {

        // 공공 공간 먼저 조회
        List<StudySpace> publicSpaces = studySpaceRepository.findPublicSpacesNearby(lat, lng, effectiveRadius);
        List<StudySpaceResponse> publicResponses = publicSpaces.stream()
                .map(space -> {
                    double dist = haversine(lat, lng,
                            space.getLatitude().doubleValue(),   // BigDecimal → double
                            space.getLongitude().doubleValue()); // -> BigDecimal은 수학 계산 메서드에 바로 못 넣음
                    return StudySpaceResponse.from(space, dist);
                })
                .sorted((a, b) -> Integer.compare(a.getDistanceMeters(), b.getDistanceMeters()))
                .toList();

        int privateSlots = Math.min(MIN_PRIVATE_SPACE_COUNT, MAX_RESULT_COUNT);
        int publicLimit = Math.max(MAX_RESULT_COUNT - privateSlots, 0);

        List<StudySpaceResponse> limitedPublicResponses = publicResponses.stream()
                .limit(publicLimit)
                .toList();

        List<StudySpaceResponse> privateResponses = kakaoLocalClient
                .searchNearbyPrivateSpaces(lat, lng, effectiveRadius, privateSlots)
                .stream()
                .map(place -> StudySpaceResponse.builder()
                        .id("kakao:" + place.id())
                        .name(place.place_name())
                        .type(StudySpace.SpaceType.CAFE.name())
                        .address(place.resolvedAddress())
                        .latitude(new BigDecimal(place.y()))
                        .longitude(new BigDecimal(place.x()))
                        .operatingHours(null)
                        .phone(place.phone())
                        .hasWifi(null)
                        .hasPowerOutlet(null)
                        .distanceMeters(place.distanceMeters())
                        .build())
                .toList();

        int remainingSlots = MAX_RESULT_COUNT - limitedPublicResponses.size();
        List<StudySpaceResponse> limitedPrivateResponses = privateResponses.stream()
                .limit(Math.max(remainingSlots, 0))
                .toList();

        List<StudySpaceResponse> result = new ArrayList<>(limitedPublicResponses);
        result.addAll(limitedPrivateResponses);
        return result;
    }

    private List<StudySpaceResponse> getStoredSpacesByType(double lat, double lng, int effectiveRadius, StudySpace.SpaceType type) {
        return studySpaceRepository.findSpacesNearbyByType(lat, lng, effectiveRadius, type.name()).stream()
                .map(space -> toResponse(space, lat, lng))
                .sorted((a, b) -> Integer.compare(a.getDistanceMeters(), b.getDistanceMeters()))
                .limit(MAX_RESULT_COUNT)
                .toList();
    }

    private List<StudySpaceResponse> getNearbyCafeSpaces(double lat, double lng, int effectiveRadius) {
        return kakaoLocalClient.searchNearbyPrivateSpaces(lat, lng, effectiveRadius, MAX_RESULT_COUNT).stream()
                .map(place -> StudySpaceResponse.builder()
                        .id("kakao:" + place.id())
                        .name(place.place_name())
                        .type(StudySpace.SpaceType.CAFE.name())
                        .address(place.resolvedAddress())
                        .latitude(new BigDecimal(place.y()))
                        .longitude(new BigDecimal(place.x()))
                        .operatingHours(null)
                        .phone(place.phone())
                        .hasWifi(null)
                        .hasPowerOutlet(null)
                        .distanceMeters(place.distanceMeters())
                        .build())
                .toList();
    }

    private StudySpaceResponse toResponse(StudySpace space, double lat, double lng) {
        double dist = haversine(lat, lng,
                space.getLatitude().doubleValue(),
                space.getLongitude().doubleValue());
        return StudySpaceResponse.from(space, dist);
    }

    // Haversine 공식: 두 위경도 좌표 사이의 지표면 거리(미터) 계산
    // 지구 반지름 6,371,000m 기준
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

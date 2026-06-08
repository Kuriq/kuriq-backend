package com.example.kuriq.service;

import com.example.kuriq.dto.space.response.StudySpaceResponse;
import com.example.kuriq.entity.space.StudySpace;
import com.example.kuriq.repository.space.StudySpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이라 readOnly → 성능 최적화
public class StudySpaceService {

    private final StudySpaceRepository studySpaceRepository;

    // 반경 기본값: 2000m / 최대값: 5000m
    private static final int DEFAULT_RADIUS = 2000;
    private static final int MAX_RADIUS     = 5000;

    // 공공 공간이 이 수 미만이면 카페를 보조로 추가
    private static final int PUBLIC_SPACE_MIN = 3;

    // 위치 기반 주변 학습 공간 조회
    // 처리 순서:
    //   1) 공공 공간(도서관, 평생학습관, 50플러스) 먼저 조회
    //   2) 공공 공간이 3개 미만이면 카페를 추가로 조회해서 합침
    //   3) 합친 목록을 거리순으로 재정렬 후 최대 20개 반환
    public List<StudySpaceResponse> getNearbySpaces(double lat, double lng, Integer radius) {

        // 반경 유효성 처리: null이면 기본값, MAX_RADIUS 초과 시 최대값으로 고정
        int effectiveRadius = (radius == null) ? DEFAULT_RADIUS : Math.min(radius, MAX_RADIUS);

        // 공공 공간 먼저 조회
        List<StudySpace> publicSpaces = studySpaceRepository.findPublicSpacesNearby(lat, lng, effectiveRadius);
        List<StudySpace> result = new ArrayList<>(publicSpaces);

        // 공공 공간이 기준치 미만이면 카페를 보조로 추가
        if (publicSpaces.size() < PUBLIC_SPACE_MIN) {
            List<StudySpace> cafes = studySpaceRepository.findCafesNearby(lat, lng, effectiveRadius);
            result.addAll(cafes);
        }

        // 각 공간까지의 거리를 Haversine으로 계산 후 DTO 변환
        // 공공 + 카페 합쳤을 때 전체 거리순 보장을 위해 재정렬
        return result.stream()
                .map(space -> {
                    double dist = haversine(lat, lng,
                            space.getLatitude().doubleValue(),   // BigDecimal → double
                            space.getLongitude().doubleValue()); // -> BigDecimal은 수학 계산 메서드에 바로 못 넣음
                    return StudySpaceResponse.from(space, dist);
                })
                .sorted((a, b) -> Integer.compare(a.getDistanceMeters(), b.getDistanceMeters()))
                .limit(20) // 최대 20개 제한
                .toList();
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

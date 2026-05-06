package com.example.kuriq.repository.space;

import com.example.kuriq.entity.space.StudySpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudySpaceRepository extends JpaRepository<StudySpace, String> {

    // 반경 내 공공 공간(도서관, 평생학습관, 50플러스)만 거리순 조회 (최대 20개)
    // 카페는 규칙상 따로 조회
    // 여기서는 반경 내 공간만 조회, 거리 계산은 service에서
    @Query(value = """
        SELECT *
        FROM study_spaces
        WHERE is_active = true  -- 활성화된 공간만
          AND type != 'CAFE'
          AND (
            6371000 * acos(
                cos(radians(:lat)) * cos(radians(latitude))
                * cos(radians(longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(latitude))
            )
          ) <= :radius  -- 반경 제한 ex) radius == 3000 이면 3km 이내만 조회
        """, nativeQuery = true)
    List<StudySpace> findPublicSpacesNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") int radius
    );

    // 반경 내 카페만 거리순 조회
    // 공공 공간이 3개 미만일 때 보조로 추가
    @Query(value = """
        SELECT *
        FROM study_spaces
        WHERE is_active = true
          AND type = 'CAFE'
          AND (
            6371000 * acos(
                cos(radians(:lat)) * cos(radians(latitude))
                * cos(radians(longitude) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(latitude))
            )
          ) <= :radius
        """, nativeQuery = true)
    List<StudySpace> findCafesNearby(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") int radius
    );
}

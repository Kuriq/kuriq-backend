package com.example.kuriq.controller;

import com.example.kuriq.dto.space.response.StudySpaceResponse;
import com.example.kuriq.service.StudySpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "StudySpace", description = "학습 공간 API")
@SecurityRequirement(name = "bearerAuth") // Swagger에서 JWT 인증 필요한 API라고 표시됨
@RestController
@RequestMapping("/api/v1/spaces")
@RequiredArgsConstructor
@Validated  // min이 동작하게 하기 위한 어노테이션
public class StudySpaceController {

    private final StudySpaceService studySpaceService;

    // GET /api/v1/spaces/nearby?lat=37.5&lng=127.0&radius=2000
    // 현재 위치 기반으로 주변 학습 공간을 거리순으로 최대 20개 반환
    @Operation(
            summary = "주변 학습 공간 조회",
            description = "현재 위치(위도/경도) 기준 반경 내 학습 공간을 거리순으로 반환합니다. 최대 20개."
    )
    @GetMapping("/nearby")
    public ResponseEntity<List<StudySpaceResponse>> getNearbySpaces(
            @Parameter(description = "현재 위치 위도", example = "37.5172", required = true) // swagger 문서 설명
            @RequestParam double lat,

            @Parameter(description = "현재 위치 경도", example = "127.0473", required = true)
            @RequestParam double lng,

            @Parameter(description = "검색 반경(m). 기본 2000, 최대 10000", example = "2000")
            @RequestParam(required = false) @Min(value = 1, message = "반경은 1m 이상이어야 합니다") Integer radius,

            // JWT 인증에서 추출한 userId — 인증 확인 용도 (실제 조회에는 미사용)
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(studySpaceService.getNearbySpaces(lat, lng, radius));
    }
}

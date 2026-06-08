package com.example.kuriq.controller;

import com.example.kuriq.dto.roadmap.request.RoadmapGenerateRequest;
import com.example.kuriq.dto.roadmap.response.RoadmapResponse;
import com.example.kuriq.service.RoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@SecurityRequirement(name = "bearerAuth")

@Tag(name = "Roadmap", description = "로드맵 API")
@RestController
@RequestMapping("/api/v1/roadmap")
@RequiredArgsConstructor
public class RoadmapController {

    private final RoadmapService roadmapService;

    // AI가 사용자 입력(prompt)을 기반으로 학습 로드맵 생성
    @Operation(summary = "AI 로드맵 생성")
    @PostMapping("/generate")
    public ResponseEntity<RoadmapResponse> generate(
            @Valid @RequestBody RoadmapGenerateRequest req,
            @AuthenticationPrincipal String userId) {

        System.out.println("userId: " + userId);

        // prompt + userId를 service에 전달해서 로드맵 생성 후 반환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roadmapService.generateRoadmap(req.getPrompt(), userId));
    }

    // 로그인한 사용자의 로드맵 목록 조회 (페이징 처리)
    @Operation(summary = "내 로드맵 목록 조회")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyRoadmaps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal String userId) {

        // 사용자별 로드맵 리스트 조회
        List<RoadmapResponse> list =
                roadmapService.getMyRoadmaps(userId, PageRequest.of(page, size));

        // content + 페이지 정보 같이 반환
        return ResponseEntity.ok(Map.of(
                "content", list,
                "currentPage", page,
                "size", size
        ));
    }

    // 특정 로드맵 상세 조회 (본인 것만 조회 가능)
    @Operation(summary = "로드맵 상세 조회")
    @GetMapping("/{roadmapId}")
    public ResponseEntity<RoadmapResponse> getRoadmap(
            @PathVariable String roadmapId,
            @AuthenticationPrincipal String userId) {

        // roadmapId + userId로 검증 후 상세 데이터 반환
        return ResponseEntity.ok(
                roadmapService.getRoadmapById(roadmapId, userId)
        );
    }

    // 특정 로드맵을 활성화 (현재 진행 중인 로드맵으로 설정)
    @Operation(summary = "로드맵 활성화")
    @PatchMapping("/{roadmapId}/activate")
    public ResponseEntity<RoadmapResponse> activate(
            @PathVariable String roadmapId,
            @AuthenticationPrincipal String userId) {

        // 기존 활성 로드맵 비활성화 후, 해당 로드맵 활성화
        return ResponseEntity.ok(
                roadmapService.activateRoadmap(roadmapId, userId)
        );
    }

    // 특정 로드맵 삭제 (본인 것만 삭제 가능)
    @Operation(summary = "로드맵 삭제")
    @DeleteMapping("/{roadmapId}")
    public ResponseEntity<Void> delete(
            @PathVariable String roadmapId,
            @AuthenticationPrincipal String userId) {

        // 로드맵 삭제 실행
        roadmapService.deleteRoadmap(roadmapId, userId);

        // 성공 시 204 No Content 반환
        return ResponseEntity.noContent().build();
    }
}

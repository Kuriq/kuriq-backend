package com.example.kuriq.controller;

import com.example.kuriq.dto.roadmap.request.ProgressCompleteRequest;
import com.example.kuriq.dto.roadmap.response.RoadmapResponse;
import com.example.kuriq.service.RoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@SecurityRequirement(name = "bearerAuth")

@Tag(name = "Progress", description = "학습 진도 API")
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {
    private final RoadmapService roadmapService;
    // 강좌 완료 체크
    @Operation(summary = "강좌 완료 체크")
    @PostMapping("/complete")
    public ResponseEntity<RoadmapResponse.RoadmapItemResponse> complete(
            @Valid @RequestBody ProgressCompleteRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(roadmapService.completeItem(req.getRoadmapItemId(), userId));
    }
    // 강좌 완료 취소
    @Operation(summary = "강좌 완료 취소")
    @DeleteMapping("/complete/{roadmapItemId}")
    public ResponseEntity<RoadmapResponse.RoadmapItemResponse> uncomplete(
            @PathVariable String roadmapItemId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(roadmapService.uncompleteItem(roadmapItemId, userId));
    }
}

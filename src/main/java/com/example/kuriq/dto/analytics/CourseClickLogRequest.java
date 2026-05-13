package com.example.kuriq.dto.analytics;

import com.example.kuriq.entity.analytics.CourseClickLog;
import com.example.kuriq.entity.roadmap.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

// 클릭 로그 요청 시 프론트에서 보내는 데이터
@Getter
public class CourseClickLogRequest {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "강좌 ID를 입력해 주세요")
    private String courseId; // 클릭된 강좌 ID

    @Schema(example = "K_MOOC", description = "K_MOOC / KOCW / LLL_PORTAL / SEOUL_LLL")
    @NotNull(message = "플랫폼을 입력해 주세요")
    private Platform platform; // 정의된 값 외 입력 시 Spring이 자동으로 400 반환

    @Schema(example = "roadmap", description = "roadmap / search / dashboard / recommendation / history")
    @NotNull(message = "클릭 출처를 입력해 주세요")
    private CourseClickLog.ClickSource source; // 어느 화면에서 클릭했는지
}


package com.example.kuriq.dto.course.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "강좌 검색 요청 파라미터")
public class CourseSearchRequest {

    @Schema(description = "검색어. 강좌명, 기관명, 설명에서 검색합니다.", example = "파이썬")
    private String keyword;

    @Schema(description = "제공 플랫폼", example = "kuriq-test")
    private String platform;

    @Schema(description = "난이도", example = "초급")
    private String difficulty;

    @Schema(description = "카테고리", example = "프로그래밍")
    private String category;

    @Schema(description = "수강 기간 범위. 0-4, 5-8, 9-12, 13+ 또는 숫자 기반 min-max/min+ 형식을 지원합니다.", example = "0-4")
    private String durationRange;

    @Schema(description = "수료증 제공 여부", example = "false")
    private Boolean hasCertificate;

    @Schema(description = "정렬 기준: latest, title, duration, hours, popular", example = "latest", defaultValue = "latest")
    private String sort = "latest";

    @Min(0)
    @Schema(description = "페이지 번호. 0부터 시작합니다.", example = "0", defaultValue = "0", minimum = "0")
    private int page = 0;

    @Min(1)
    @Max(100)
    @Schema(description = "페이지 크기", example = "20", defaultValue = "20", minimum = "1", maximum = "100")
    private int size = 20;
}

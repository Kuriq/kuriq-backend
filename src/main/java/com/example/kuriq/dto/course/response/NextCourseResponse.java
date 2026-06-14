package com.example.kuriq.dto.course.response;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.util.CoursePlatformLabelResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "다음 추천 강좌 응답")
public class NextCourseResponse {

    @Schema(description = "추천 강좌 ID", example = "abc123")
    private String courseId;

    @Schema(description = "강좌 제목", example = "머신러닝 입문")
    private String title;

    @Schema(description = "운영 기관", example = "서울대학교")
    private String institution;

    @Schema(description = "플랫폼", example = "K-MOOC")
    private String platform;

    @Schema(description = "카테고리", example = "IT/SW")
    private String category;

    @Schema(description = "난이도", example = "초급")
    private String difficulty;

    @Schema(description = "예상 학습 시간(시간)", example = "12.0")
    private BigDecimal estimatedHours;

    @Schema(description = "수강 신청 URL", example = "https://www.kmooc.kr/...")
    private String url;

    @Schema(description = "수료증 제공 여부", example = "true")
    private Boolean hasCertificate;

    @Schema(description = "큐리 추천 메시지", example = "파이썬 기초를 잘 마무리했어요! 다음으로 머신러닝 입문 과정은 어떨까요?")
    private String message;

    public static NextCourseResponse from(Course course, String message) {
        return NextCourseResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .institution(CoursePlatformLabelResolver.normalizeInstitution(course.getInstitution(), course.getPlatform()))
                .platform(CoursePlatformLabelResolver.resolvePlatform(course.getPlatform(), course.getInstitution()))
                .category(course.getCategory())
                .difficulty(course.getDifficulty())
                .estimatedHours(course.getEstimatedHours())
                .url(course.getUrl())
                .hasCertificate(course.getHasCertificate())
                .message(message)
                .build();
    }
}

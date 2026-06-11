package com.example.kuriq.dto.course.response;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.util.CoursePlatformLabelResolver;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CourseResponse {
    private String id;
    private String title;
    private String platform;
    private String institution;
    private String category;
    private String difficulty;
    private Integer durationWeeks;
    private BigDecimal estimatedHours;
    private Boolean hasCertificate;
    private String url;

    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .platform(CoursePlatformLabelResolver.resolvePlatform(course.getPlatform(), course.getInstitution()))
                .institution(CoursePlatformLabelResolver.normalizeInstitution(course.getInstitution(), course.getPlatform()))
                .category(course.getCategory())
                .difficulty(course.getDifficulty())
                .durationWeeks(course.getDurationWeeks())
                .estimatedHours(course.getEstimatedHours())
                .hasCertificate(course.getHasCertificate())
                .url(course.getUrl())
                .build();
    }
}

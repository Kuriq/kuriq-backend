package com.example.kuriq.dto.course.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseSearchRequest {
    private String keyword;
    private String platform;
    private String difficulty;
    private String category;
    private String durationRange;
    private Boolean hasCertificate;
    private String sort = "latest";

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 20;
}

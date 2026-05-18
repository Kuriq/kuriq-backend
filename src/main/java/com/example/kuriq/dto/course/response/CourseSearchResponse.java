package com.example.kuriq.dto.course.response;

import com.example.kuriq.entity.roadmap.Course;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class CourseSearchResponse {
    private List<CourseResponse> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
    private boolean hasNext;

    public static CourseSearchResponse from(Page<Course> page) {
        return CourseSearchResponse.builder()
                .content(page.getContent().stream().map(CourseResponse::from).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .build();
    }
}

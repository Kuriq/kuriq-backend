package com.example.kuriq.controller;

import com.example.kuriq.dto.course.request.CourseSearchRequest;
import com.example.kuriq.dto.course.response.CourseSearchResponse;
import com.example.kuriq.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course", description = "강좌 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(summary = "강좌 검색")
    @GetMapping("/search")
    public ResponseEntity<CourseSearchResponse> search(@Valid @ModelAttribute CourseSearchRequest request) {
        return ResponseEntity.ok(courseService.search(request));
    }
}

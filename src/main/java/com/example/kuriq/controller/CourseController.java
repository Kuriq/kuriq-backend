package com.example.kuriq.controller;

import com.example.kuriq.dto.course.request.CourseSearchRequest;
import com.example.kuriq.dto.course.response.CourseSearchResponse;
import com.example.kuriq.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course", description = "강좌 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Operation(
            summary = "강좌 검색",
            description = "검색어, 플랫폼, 난이도, 카테고리, 기간, 수료증 여부, 정렬, 페이징 조건으로 활성 강좌를 검색합니다."
    )
    @GetMapping("/search")
    public ResponseEntity<CourseSearchResponse> search(
            @Valid @ParameterObject @ModelAttribute CourseSearchRequest request) {
        return ResponseEntity.ok(courseService.search(request));
    }
}

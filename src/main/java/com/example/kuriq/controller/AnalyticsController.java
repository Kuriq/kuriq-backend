package com.example.kuriq.controller;

import com.example.kuriq.dto.analytics.CourseClickLogRequest;
import com.example.kuriq.event.CourseClickEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Tag(name = "Analytics", description = "분석 API")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;

    @Operation(summary = "강좌 클릭 로깅", description = "수강 신청 버튼 클릭 시 호출. 비로그인도 허용.")
    @PostMapping("/course-click")
    public ResponseEntity<Void> logCourseClick(
            @Valid @RequestBody CourseClickLogRequest req,
            @AuthenticationPrincipal String userId
    ) {
        // 이벤트 발행만 하고 즉시 204 반환
        // 실제 DB 저장 + Redis 집계는 리스너가 별도 스레드에서 처리
        eventPublisher.publishEvent(new CourseClickEvent(
                userId,
                req.getCourseId(),
                req.getPlatform(),
                req.getSource()
        ));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "오늘의 인기 강좌 TOP N 조회",
            description = "Redis Sorted Set 기반. DB 집계 쿼리 없이 즉시 반환.")
    @GetMapping("/courses/popular")
    public ResponseEntity<Map<String, Double>> getPopularCourses(
            @RequestParam(defaultValue = "10") int limit // 기본 TOP 10
    ) {
        String today = LocalDate.now().toString();
        String key = "popular:courses:" + today;

        // 점수 높은 순으로 limit개 조회 (ZRANGE ... REV WITHSCORES)
        Set<ZSetOperations.TypedTuple<String>> result =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        // courseId -> 클릭 수 형태의 Map으로 변환 (순서 보장을 위해 LinkedHashMap)
        Map<String, Double> popular = new LinkedHashMap<>();
        if (result != null) {
            result.forEach(tuple -> popular.put(tuple.getValue(), tuple.getScore()));
        }

        return ResponseEntity.ok(popular);
    }
}


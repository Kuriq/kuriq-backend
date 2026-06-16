package com.example.kuriq.controller;

import com.example.kuriq.dto.analytics.CourseClickLogRequest;
import com.example.kuriq.dto.course.response.CourseResponse;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.event.CourseClickEvent;
import com.example.kuriq.repository.roadmap.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Analytics", description = "분석 API")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final CourseRepository courseRepository;
    private static final String SESSION_COOKIE = "anon_session";

    @Operation(summary = "강좌 클릭 로깅", description = "수강 신청 버튼 클릭 시 호출. 비로그인도 허용.")
    @PostMapping("/course-click")
    public ResponseEntity<Void> logCourseClick(
            @Valid @RequestBody CourseClickLogRequest req,
            @AuthenticationPrincipal String userId,
            HttpServletRequest httpReq,
            HttpServletResponse httpRes
    ) {
        if (userId == null) {
            String sessionId = getSessionId(httpReq);
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                setSessionCookie(httpRes, sessionId);
            }

            String dedupKey = "click:dedup:anon:" + sessionId + ":" + req.getCourseId() + ":" + LocalDate.now();
            Boolean isFirst = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", Duration.ofDays(1));

            if (Boolean.FALSE.equals(isFirst)) {
                return ResponseEntity.noContent().build();
            }
        }

        eventPublisher.publishEvent(new CourseClickEvent(
                userId,
                req.getCourseId(),
                req.getPlatform(),
                req.getSource()
        ));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "오늘의 인기 강좌 TOP N 조회",
            description = "Redis Sorted Set 기반. 클릭 수 높은 순으로 강좌 전체 정보를 반환.")
    @GetMapping("/courses/popular")
    public ResponseEntity<List<CourseResponse>> getPopularCourses(
            @RequestParam(defaultValue = "20") int limit
    ) {
        String today = LocalDate.now().toString();
        String key = "popular:courses:" + today;

        Set<ZSetOperations.TypedTuple<String>> result =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (result == null || result.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<String> courseIds = result.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < courseIds.size(); i++) {
            orderMap.put(courseIds.get(i), i);
        }

        List<CourseResponse> response = courseRepository.findAllById(courseIds).stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .sorted(Comparator.comparingInt(c -> orderMap.getOrDefault(c.getId(), Integer.MAX_VALUE)))
                .map(CourseResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private String getSessionId(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> SESSION_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setSessionCookie(HttpServletResponse res, String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(24 * 60 * 60);
        res.addCookie(cookie);
    }
}

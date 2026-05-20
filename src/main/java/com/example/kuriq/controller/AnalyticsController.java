package com.example.kuriq.controller;

import com.example.kuriq.dto.analytics.CourseClickLogRequest;
import com.example.kuriq.event.CourseClickEvent;
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

@Tag(name = "Analytics", description = "분석 API")
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private static final String SESSION_COOKIE = "anon_session"; // 비로그인 사용자 세션 ID 쿠키 이름

    @Operation(summary = "강좌 클릭 로깅", description = "수강 신청 버튼 클릭 시 호출. 비로그인도 허용.")
    @PostMapping("/course-click")
    public ResponseEntity<Void> logCourseClick(
            @Valid @RequestBody CourseClickLogRequest req,
            @AuthenticationPrincipal String userId,
            HttpServletRequest httpReq,   // 쿠키 읽기용
            HttpServletResponse httpRes   // 쿠키 쓰기용
    ) {
        if (userId == null) {

            // 쿠키에서 세션 ID 꺼내기 (없으면 새로 생성)
            String sessionId = getSessionId(httpReq);
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString(); // 랜덤 UUID 생성
                setSessionCookie(httpRes, sessionId);     // 쿠키에 저장 (24시간)
            }

            // Redis에 "오늘 이 세션이 이 강좌를 클릭했다" 기록
            // SET NX: 없을 때만 저장 -> true면 첫 클릭, false면 중복
            String dedupKey = "click:dedup:anon:" + sessionId + ":" + req.getCourseId() + ":" + LocalDate.now();
            Boolean isFirst = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", Duration.ofDays(1));

            // 오늘 이미 클릭한 강좌면 집계하지 않고 204 반환
            if (Boolean.FALSE.equals(isFirst)) {
                return ResponseEntity.noContent().build();
            }
        }

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

    // 쿠키에서 세션 ID 꺼내기
    private String getSessionId(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> SESSION_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // 세션 ID를 쿠키에 저장 (24시간)
    // Redis 중복 방지 키 TTL(24시간)과 동일하게 맞춤
    private void setSessionCookie(HttpServletResponse res, String sessionId) {
        Cookie cookie = new Cookie(SESSION_COOKIE, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);        // JS 접근 차단 (보안)
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        res.addCookie(cookie);
    }
}


package com.example.kuriq.event;

import com.example.kuriq.entity.analytics.CourseClickLog;
import com.example.kuriq.repository.analytics.CourseClickLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

// CourseClickEvent를 받아서 DB 저장 + Redis 인기 집계를 처리하는 리스너
// @Async 덕분에 별도 스레드에서 실행됨 -> 컨트롤러는 저장 완료를 기다리지 않음
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseClickEventListener {

    private final CourseClickLogRepository courseClickLogRepository;
    private final StringRedisTemplate redisTemplate; // Redis 조작 도구

    @Async          // 별도 스레드에서 실행 (DB/Redis 작업이 느려도 API 응답에 영향 없음)
    @EventListener  // 이 메서드가 이벤트 수신자임을 Spring에 알림
    public void handle(CourseClickEvent event) {
        saveToDb(event);
        // 로그인 사용자는 중복 체크 후 집계
        // 비로그인 사용자(userId == null)는 쿠키로 중복 방지 (컨트롤러에서 처리)
        if (event.getUserId() != null) {
            if (!isDuplicate(event.getUserId(), event.getCourseId())) {
                incrementPopularScore(event.getCourseId());
            }
        } else {
            // 비로그인은 쿠키 체크를 이미 통과한 경우만 이벤트가 발행됨
            incrementPopularScore(event.getCourseId());
        }
    }

    // 1. DB에 클릭 로그 저장(중복 여부와 관계없이 항상 저장 — 원시 데이터 보존)
    private void saveToDb(CourseClickEvent event) {
        try {
            CourseClickLog log = CourseClickLog.create(
                    event.getUserId(),
                    event.getCourseId(),
                    event.getPlatform(),
                    event.getSource()
            );
            courseClickLogRepository.save(log);
        } catch (Exception e) {
            log.error("클릭 로그 DB 저장 실패 - courseId: {}, error: {}",
                    event.getCourseId(), e.getMessage());
        }
    }

    // 2. 로그인 사용자 중복 클릭 체크
    // SET NX: 키가 없을 때만 저장 (없으면 true 반환 = 첫 클릭, 있으면 false = 중복)
    // TTL 24시간 -> 자정 이후엔 키가 사라져서 다음날 다시 집계 가능
    private boolean isDuplicate(String userId, String courseId) {
        String today = LocalDate.now().toString();
        String dedupKey = "click:dedup:" + userId + ":" + courseId + ":" + today;

        // setIfAbsent = SET NX (없을 때만 저장)
        // true 반환 => 키가 없었음 => 첫 클릭 => 중복 아님
        // false 반환 => 키가 이미 있었음 => 오늘 이미 클릭함 => 중복
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofDays(1));

        return Boolean.FALSE.equals(isFirst); // false면 중복
    }

    // 3. Redis Sorted Set에 해당 강좌 클릭 수 +1
    // Key 예시: popular:courses:2026-05-13
    // 날짜별로 관리하고 7일 후 자동 삭제
    private void incrementPopularScore(String courseId) {
        try {
            String today = LocalDate.now().toString();
            String key = "popular:courses:" + today;

            // courseId의 점수를 1 증가 (없으면 1로 시작)
            redisTemplate.opsForZSet().incrementScore(key, courseId, 1);

            // 이 key가 처음 만들어질 때(-1)만 TTL 7일 설정
            // (이미 TTL이 있으면 덮어쓰지 않기 위해 조건 체크)
            if (redisTemplate.getExpire(key) == -1) {
                redisTemplate.expire(key, Duration.ofDays(7));
            }
        } catch (Exception e) {
            // Redis 실패해도 DB 저장에는 영향 없음
            log.error("Redis 인기 집계 실패 - courseId: {}, error: {}",
                    courseId, e.getMessage());
        }
    }
}

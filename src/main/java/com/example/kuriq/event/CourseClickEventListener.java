package com.example.kuriq.event;

import com.example.kuriq.entity.analytics.CourseClickLog;
import com.example.kuriq.repository.analytics.CourseClickLogRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseClickEventListener {

    private final CourseClickLogRepository courseClickLogRepository;
    private final CourseRepository courseRepository;
    private final StringRedisTemplate redisTemplate;

    @Async
    @EventListener
    public void handle(CourseClickEvent event) {
        saveToDb(event);
        if (event.getUserId() != null) {
            if (!isDuplicate(event.getUserId(), event.getCourseId())) {
                incrementPopularScore(event.getCourseId());
            }
        } else {
            incrementPopularScore(event.getCourseId());
        }
    }

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

    private boolean isDuplicate(String userId, String courseId) {
        String today = LocalDate.now().toString();
        String dedupKey = "click:dedup:" + userId + ":" + courseId + ":" + today;
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofDays(1));
        return Boolean.FALSE.equals(isFirst);
    }

    private void incrementPopularScore(String courseId) {
        try {
            String today = LocalDate.now().toString();
            String key = "popular:courses:" + today;
            redisTemplate.opsForZSet().incrementScore(key, courseId, 1);
            if (redisTemplate.getExpire(key) == -1) {
                redisTemplate.expire(key, Duration.ofDays(7));
            }
        } catch (Exception e) {
            log.error("Redis 인기 집계 실패 - courseId: {}, error: {}",
                    courseId, e.getMessage());
        }
        try {
            courseRepository.incrementClickCount(courseId);
        } catch (Exception e) {
            log.error("MySQL clickCount 업데이트 실패 - courseId: {}, error: {}",
                    courseId, e.getMessage());
        }
    }
}

package com.example.kuriq.repository.analytics;

import com.example.kuriq.entity.analytics.CourseClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

// 지금은 save()만 쓰지만, 나중에 분석 쿼리가 생기면 여기에 메서드를 추가하면 됨
public interface CourseClickLogRepository extends JpaRepository<CourseClickLog, Long> {
}

package com.example.kuriq.event;

import com.example.kuriq.entity.analytics.CourseClickLog;
import com.example.kuriq.entity.roadmap.Platform;
import lombok.Getter;

// 클릭이 발생했다는 사실을 담는 이벤트 클래스
// 컨트롤러 -> 이벤트 발행 -> 리스너가 받아서 DB 저장
// (이벤트 방식을 쓰는 이유: 컨트롤러가 DB 저장 완료를 기다리지 않고 즉시 응답 반환 가능)
@Getter
public class CourseClickEvent {

    private final String userId;      // 클릭한 사용자 ID (비로그인이면 null)
    private final String courseId;    // 클릭된 강좌 ID
    private final Platform platform;  // 강좌 플랫폼
    private final CourseClickLog.ClickSource source; // 어느 화면에서 클릭했는지

    public CourseClickEvent(String userId, String courseId,
                            Platform platform, CourseClickLog.ClickSource source) {
        this.userId = userId;
        this.courseId = courseId;
        this.platform = platform;
        this.source = source;
    }
}


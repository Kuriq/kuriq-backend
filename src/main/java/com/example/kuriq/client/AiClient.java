package com.example.kuriq.client;

import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiClient {

    @Getter
    @Builder
    public static class RoadmapGenerateAiRequest {
        private String prompt;
        private String userId;
    }

    @Getter
    @Setter
    public static class RoadmapGenerateAiResponse {
        private String goal;
        private int totalWeeks;
        private int weeklyHours;
        private List<WeekDto> weeks;

        @Getter
        @Setter
        public static class WeekDto {
            private int weekNumber;
            private String title;
            private String description;
            private double totalHours;
            private List<CourseItemDto> courses;
        }

        @Getter
        @Setter
        public static class CourseItemDto {
            private String courseId;
            private int orderInWeek;
        }
    }

    // AI 서버 연동 전 임시 응답
    public RoadmapGenerateAiResponse generateRoadmap(RoadmapGenerateAiRequest request) {
        // 1주차 강좌 세팅
        RoadmapGenerateAiResponse.CourseItemDto course1 = new RoadmapGenerateAiResponse.CourseItemDto();
        course1.setCourseId("aaaaaaaa-0000-0000-0000-000000000001");
        course1.setOrderInWeek(1);

        RoadmapGenerateAiResponse.WeekDto week1 = new RoadmapGenerateAiResponse.WeekDto();
        week1.setWeekNumber(1);
        week1.setTitle("1주차: 파이썬 기초");
        week1.setDescription("파이썬 문법을 익히며 프로그래밍 사고를 시작합니다");
        week1.setTotalHours(5.0);
        week1.setCourses(List.of(course1));

        // 2주차 강좌 세팅
        RoadmapGenerateAiResponse.CourseItemDto course2 = new RoadmapGenerateAiResponse.CourseItemDto();
        course2.setCourseId("aaaaaaaa-0000-0000-0000-000000000002");
        course2.setOrderInWeek(1);

        RoadmapGenerateAiResponse.WeekDto week2 = new RoadmapGenerateAiResponse.WeekDto();
        week2.setWeekNumber(2);
        week2.setTitle("2주차: 데이터 사이언스 입문");
        week2.setDescription("데이터 분석의 기초 개념을 학습합니다");
        week2.setTotalHours(5.0);
        week2.setCourses(List.of(course2));

        RoadmapGenerateAiResponse response = new RoadmapGenerateAiResponse();
        response.setGoal("파이썬 기초부터 데이터 분석까지 학습");
        response.setTotalWeeks(2);
        response.setWeeklyHours(5);
        response.setWeeks(List.of(week1, week2));
        return response;
    }
}
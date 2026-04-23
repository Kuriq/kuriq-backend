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

    // TODO: AI 서버 연동 전 임시 응답
    public RoadmapGenerateAiResponse generateRoadmap(RoadmapGenerateAiRequest request) {
        RoadmapGenerateAiResponse response = new RoadmapGenerateAiResponse();
        response.setGoal("임시 학습 목표");
        response.setTotalWeeks(4);
        response.setWeeklyHours(5);
        response.setWeeks(List.of());
        return response;
    }
}
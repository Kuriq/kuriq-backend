package com.example.kuriq.client;

import lombok.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;


@Component
@RequiredArgsConstructor
public class AiClient {

    private final WebClient aiWebClient;

    @Getter
    @Builder
    public static class RoadmapGenerateAiRequest {
        private String prompt;
        private String userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapGenerateAiResponse {
        private String title;
        private String goal;
        private int totalWeeks;
        private int weeklyHours;
        private List<WeekDto> weeks;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WeekDto {
            private int weekNumber;
            private String title;
            private String description;
            private double totalHours;
            private List<CourseItemDto> courses;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CourseItemDto {
            private String courseId;
            private int orderInWeek;
        }
    }

    // AI 서버 연동용
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuizGenerateAiRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("noteContent")
        private String noteContent;
        @com.fasterxml.jackson.annotation.JsonProperty("courseTitle")
        private String courseTitle;
        @com.fasterxml.jackson.annotation.JsonProperty("courseDifficulty")
        private String courseDifficulty;
        @com.fasterxml.jackson.annotation.JsonProperty("excludeQuestions")
        private List<String> excludeQuestions;
        @com.fasterxml.jackson.annotation.JsonProperty("questionCount")
        private Integer questionCount;
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private String userId;
    }

    @Getter
    @Setter
    public static class QuizGenerateAiResponse {
        private String courseId;
        private List<QuestionDto> questions;

        @Getter
        @Setter
        public static class QuestionDto {
            private String questionId;
            private String type;
            private String question;
            private List<OptionDto> options;
            private String correctAnswer;
            private String explanation;
            private String noteReference;
            private String weakTopic;
            private List<String> acceptableKeywords;
        }

        @Getter
        @Setter
        public static class OptionDto {
            private String id;
            private String text;
        }
    }

    @Getter
    @Builder
    public static class QuizGradeAiRequest {
        private String question;
        private String correctAnswer;
        private List<String> acceptableKeywords;
        private String userAnswer;
        private String userId;
    }

    @Getter
    @Setter
    public static class QuizGradeAiResponse {
        private String result;
        private String feedback;
        private String correctAnswer;
    }

    @Getter
    @Builder
    public static class ChatAiRequest {
        private String message;
        private String noteContent;
        private String courseTitle;
        private String courseCategory;
        private String courseDifficulty;
        private List<ChatHistoryItem> chatHistory;
        private String userId;

        @Getter
        @Builder
        public static class ChatHistoryItem {
            private String role;
            private String message;
        }
    }

    @Getter
    @Setter
    public static class ChatAiResponse {
        private String message;
        private List<String> noteReferences;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizeAiRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("noteContent")
        private String noteContent;
        @com.fasterxml.jackson.annotation.JsonProperty("courseTitle")
        private String courseTitle;
        @com.fasterxml.jackson.annotation.JsonProperty("courseCategory")
        private String courseCategory;
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private String userId;
    }

    @Getter
    @Setter
    public static class OrganizeAiResponse {
        private List<String> keywords;
        private String structuredSummary;
        private List<String> suggestions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseMetadataRequest {
        private List<String> courseIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseMetadataResponse {
        private List<CourseMetadataDto> courses;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CourseMetadataDto {
            private String courseId;
            private String title;
            private String platform;
            private String institution;
            private String category;
            private String difficulty;
            private Integer durationWeeks;
            private Double estimatedHours;
            private Boolean hasCertificate;
            private String url;
        }
    }

    // 다음 추천 요청 DTO
    // Spring Boot -> AI 서버로 보내는 요청
    // courseId: 본인 강좌 제외용, category: 벡터 검색 필터, top_k: 후보 수
    @Getter
    @Builder
    public static class RecommendationAiRequest {
        private String courseId;
        private String courseTitle;
        private String category;
        private int top_k;
    }

    // 다음 추천 응답 DTO
    // AI 서버 -> Spring Boot로 오는 응답
    @Getter
    @Setter
    public static class RecommendationAiResponse {
        private List<RecommendationCourseDto> courses;

        // 추천 강좌 1개
        @Getter
        @Setter
        public static class RecommendationCourseDto {
            private String course_id;
            private String title;
            private String institution;
            private String category;
            private String duration;
            private String url;
        }
    }

    public RoadmapGenerateAiResponse generateRoadmap(RoadmapGenerateAiRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/roadmap/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RoadmapGenerateAiResponse.class)
                .block(Duration.ofSeconds(60));
    }

    public QuizGenerateAiResponse generateQuiz(QuizGenerateAiRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/quiz/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QuizGenerateAiResponse.class)
                .block(Duration.ofSeconds(30));
    }

    public QuizGradeAiResponse gradeShortAnswer(QuizGradeAiRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/quiz/grade")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QuizGradeAiResponse.class)
                .block(Duration.ofSeconds(10));
    }

    public ChatAiResponse chat(ChatAiRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatAiResponse.class)
                .block(Duration.ofSeconds(10));
    }

    public OrganizeAiResponse organize(OrganizeAiRequest request) {
        // JSON 수동 생성 (Lombok 직렬화 문제 우회)
        String jsonBody = String.format(
            "{\"noteContent\":%s,\"courseTitle\":%s,\"courseCategory\":%s,\"userId\":%s}",
            toJsonString(request.getNoteContent()),
            toJsonString(request.getCourseTitle()),
            toJsonString(request.getCourseCategory()),
            toJsonString(request.getUserId())
        );
        
        System.out.println("=== Sending JSON: " + jsonBody.substring(0, Math.min(500, jsonBody.length())));
        System.out.println("=== JSON Length: " + jsonBody.length());
        
        // 디버그 엔드포인트로 먼저 테스트
        try {
            var debugResponse = aiWebClient.post()
                    .uri("/internal/ai/note/organize/debug")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));
            System.out.println("=== Debug Response: " + debugResponse);
        } catch (Exception debugErr) {
            System.out.println("=== Debug Error: " + debugErr.getMessage());
        }
        
        return aiWebClient.post()
                .uri("/internal/ai/note/organize")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(OrganizeAiResponse.class)
                .block(Duration.ofSeconds(15));
    }
    
    private String toJsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    public CourseMetadataResponse getCourseMetadata(List<String> courseIds) {
        CourseMetadataRequest request = new CourseMetadataRequest();
        request.setCourseIds(courseIds);
        
        return aiWebClient.post()
                .uri("/internal/ai/courses/metadata")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CourseMetadataResponse.class)
                .block(Duration.ofSeconds(10));
    }

    // 다음 추천 AI 서버 호출
    // 최근 이수한 강좌의 카테고리 기반으로 유사 강좌 추천 목록 받아옴
    public RecommendationAiResponse getRecommendations(RecommendationAiRequest request) {
        return aiWebClient.post()
                .uri("/internal/ai/recommendations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecommendationAiResponse.class)
                .block(Duration.ofSeconds(10));
    }

    private QuizGenerateAiResponse.OptionDto createOption(String id, String text) {
        QuizGenerateAiResponse.OptionDto option = new QuizGenerateAiResponse.OptionDto();
        option.setId(id);
        option.setText(text);
        return option;
    }


}

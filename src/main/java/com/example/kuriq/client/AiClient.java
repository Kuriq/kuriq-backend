package com.example.kuriq.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

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

    @Getter
    @Builder
    public static class QuizGenerateAiRequest {
        private String noteId;
        private List<String> excludeSessionIds;
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
        @JsonProperty("note_id")
        private String noteId;

        @JsonProperty("note_content")
        private String noteContent;

        @JsonProperty("course_metadata")
        private String courseMetadata;

        @JsonProperty("recent_history")
        private List<ChatHistoryItem> recentHistory;

        private String message;

        @JsonProperty("user_id")
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

        @JsonProperty("note_references")
        private List<String> noteReferences;
    }

    public RoadmapGenerateAiResponse generateRoadmap(RoadmapGenerateAiRequest request) {
        RoadmapGenerateAiResponse response = new RoadmapGenerateAiResponse();
        response.setGoal("임시 학습 목표");
        response.setTotalWeeks(4);
        response.setWeeklyHours(5);
        response.setWeeks(List.of());
        return response;
    }

    public QuizGenerateAiResponse generateQuiz(QuizGenerateAiRequest request) {
        QuizGenerateAiResponse response = new QuizGenerateAiResponse();
        response.setCourseId(UUID.nameUUIDFromBytes(("quiz-course:" + request.getNoteId()).getBytes(StandardCharsets.UTF_8)).toString());

        QuizGenerateAiResponse.QuestionDto q1 = new QuizGenerateAiResponse.QuestionDto();
        q1.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q1").getBytes(StandardCharsets.UTF_8)).toString());
        q1.setType("MULTIPLE_CHOICE");
        q1.setQuestion("노트에서 정리한 내용 중, 파이썬에서 변수를 생성할 때 필요한 것은?");
        q1.setOptions(List.of(
                createOption("A", "타입 선언 후 값 할당"),
                createOption("B", "값 할당만으로 생성"),
                createOption("C", "var 키워드 사용"),
                createOption("D", "new 키워드 사용")
        ));
        q1.setCorrectAnswer("B");
        q1.setExplanation("파이썬은 타입 선언 없이 값 할당만으로 변수를 만들 수 있습니다.");
        q1.setNoteReference("변수의 선언과 할당");
        q1.setWeakTopic("변수 생성 방식");

        QuizGenerateAiResponse.QuestionDto q2 = new QuizGenerateAiResponse.QuestionDto();
        q2.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q2").getBytes(StandardCharsets.UTF_8)).toString());
        q2.setType("TRUE_FALSE");
        q2.setQuestion("파이썬의 인덱스는 1부터 시작한다.");
        q2.setCorrectAnswer("false");
        q2.setExplanation("파이썬의 인덱스는 0부터 시작합니다.");
        q2.setNoteReference("리스트 인덱스");
        q2.setWeakTopic("인덱스 개념");

        QuizGenerateAiResponse.QuestionDto q3 = new QuizGenerateAiResponse.QuestionDto();
        q3.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q3").getBytes(StandardCharsets.UTF_8)).toString());
        q3.setType("SHORT_ANSWER");
        q3.setQuestion("파이썬에서 여러 값을 순서대로 저장하면서 수정도 가능한 자료형은?");
        q3.setCorrectAnswer("리스트");
        q3.setExplanation("여러 값을 순서대로 저장하고 수정 가능한 대표 자료형은 리스트입니다.");
        q3.setNoteReference("자료형 개요");
        q3.setWeakTopic("자료형 명칭");
        q3.setAcceptableKeywords(List.of("리스트", "list"));

        response.setQuestions(List.of(q1, q2, q3));
        return response;
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

    private QuizGenerateAiResponse.OptionDto createOption(String id, String text) {
        QuizGenerateAiResponse.OptionDto option = new QuizGenerateAiResponse.OptionDto();
        option.setId(id);
        option.setText(text);
        return option;
    }
}

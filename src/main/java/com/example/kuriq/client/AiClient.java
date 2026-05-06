package com.example.kuriq.client;

import lombok.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

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
        }

        @Getter
        @Setter
        public static class OptionDto {
            private String id;
            private String text;
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

    public QuizGenerateAiResponse generateQuiz(QuizGenerateAiRequest request) {
        QuizGenerateAiResponse response = new QuizGenerateAiResponse();
        response.setCourseId(UUID.nameUUIDFromBytes(("quiz-course:" + request.getNoteId()).getBytes(StandardCharsets.UTF_8)).toString());

        QuizGenerateAiResponse.QuestionDto q1 = new QuizGenerateAiResponse.QuestionDto();
        q1.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q1").getBytes(StandardCharsets.UTF_8)).toString());
        q1.setType("MULTIPLE_CHOICE");
        q1.setQuestion("노트에서 정리한 내용 중, 파이썬에서 변수를 생성할 때 필요한 것은?");
        QuizGenerateAiResponse.OptionDto o1 = createOption("A", "타입 선언 후 값 할당");
        QuizGenerateAiResponse.OptionDto o2 = createOption("B", "값 할당만으로 생성");
        QuizGenerateAiResponse.OptionDto o3 = createOption("C", "var 키워드 사용");
        QuizGenerateAiResponse.OptionDto o4 = createOption("D", "new 키워드 사용");
        q1.setOptions(List.of(o1, o2, o3, o4));

        QuizGenerateAiResponse.QuestionDto q2 = new QuizGenerateAiResponse.QuestionDto();
        q2.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q2").getBytes(StandardCharsets.UTF_8)).toString());
        q2.setType("TRUE_FALSE");
        q2.setQuestion("파이썬의 인덱스는 1부터 시작한다.");
        q2.setOptions(null);

        QuizGenerateAiResponse.QuestionDto q3 = new QuizGenerateAiResponse.QuestionDto();
        q3.setQuestionId(UUID.nameUUIDFromBytes((request.getNoteId() + ":q3").getBytes(StandardCharsets.UTF_8)).toString());
        q3.setType("SHORT_ANSWER");
        q3.setQuestion("파이썬에서 여러 값을 순서대로 저장하면서 수정도 가능한 자료형은?");
        q3.setOptions(null);

        response.setQuestions(List.of(q1, q2, q3));
        return response;
    }

    private QuizGenerateAiResponse.OptionDto createOption(String id, String text) {
        QuizGenerateAiResponse.OptionDto option = new QuizGenerateAiResponse.OptionDto();
        option.setId(id);
        option.setText(text);
        return option;
    }
}

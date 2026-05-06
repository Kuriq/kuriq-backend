package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.quiz.response.QuizHistoryResponse;
import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.entity.quiz.QuizOption;
import com.example.kuriq.entity.quiz.QuizQuestion;
import com.example.kuriq.entity.quiz.QuizQuestionType;
import com.example.kuriq.entity.quiz.QuizSession;
import com.example.kuriq.repository.quiz.QuizSessionRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final CourseRepository courseRepository;
    private final AiClient aiClient;

    public QuizGenerateResponse generate(QuizGenerateRequest request, String userId) {
        validateExcludedSessions(request.getExcludeSessionIds(), userId);

        AiClient.QuizGenerateAiResponse aiResponse = aiClient.generateQuiz(
                AiClient.QuizGenerateAiRequest.builder()
                        .noteId(request.getNoteId())
                        .excludeSessionIds(request.getExcludeSessionIds())
                        .userId(userId)
                        .build());
        validateAiResponse(aiResponse);

        QuizSession session = QuizSession.builder()
                .userId(userId)
                .courseId(aiResponse.getCourseId())
                .noteId(request.getNoteId())
                .totalQuestions(aiResponse.getQuestions().size())
                .build();

        for (int questionIndex = 0; questionIndex < aiResponse.getQuestions().size(); questionIndex++) {
            AiClient.QuizGenerateAiResponse.QuestionDto q = aiResponse.getQuestions().get(questionIndex);
            QuizQuestion question = QuizQuestion.builder()
                    .quizSession(session)
                    .type(QuizQuestionType.valueOf(q.getType()))
                    .orderIndex(questionIndex)
                    .question(q.getQuestion())
                    .build();
            if (q.getOptions() != null) {
                for (int optionIndex = 0; optionIndex < q.getOptions().size(); optionIndex++) {
                    AiClient.QuizGenerateAiResponse.OptionDto o = q.getOptions().get(optionIndex);
                    question.getOptions().add(QuizOption.builder()
                            .quizQuestion(question)
                            .optionId(o.getId())
                            .orderIndex(optionIndex)
                            .text(o.getText())
                            .build());
                }
            }
            session.getQuestions().add(question);
        }

        quizSessionRepository.save(session);

        return QuizGenerateResponse.builder()
                .quizSessionId(session.getId())
                .courseId(session.getCourseId())
                .noteId(session.getNoteId())
                .questions(session.getQuestions().stream().map(q -> QuizGenerateResponse.QuestionDto.builder()
                        .questionId(q.getId())
                        .type(q.getType().name())
                        .question(q.getQuestion())
                        .options(q.getType() == QuizQuestionType.MULTIPLE_CHOICE ? q.getOptions().stream()
                                .map(o -> QuizGenerateResponse.OptionDto.builder()
                                        .id(o.getOptionId())
                                        .text(o.getText())
                                        .build())
                                .collect(Collectors.toList()) : null)
                        .build()).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public QuizHistoryResponse history(String userId, String courseId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var sessions = courseId == null || courseId.isBlank()
                ? quizSessionRepository.findByUserId(userId, pageable)
                : quizSessionRepository.findByUserIdAndCourseId(userId, courseId, pageable);

        var content = sessions.getContent().stream().map(session -> {
            String courseTitle = courseRepository.findById(session.getCourseId())
                    .map(course -> course.getTitle())
                    .orElse(null);
            return QuizHistoryResponse.Item.from(session, courseTitle);
        }).toList();

        return QuizHistoryResponse.from(new PageImpl<>(content, pageable, sessions.getTotalElements()));
    }

    private void validateExcludedSessions(List<String> excludeSessionIds, String userId) {
        if (excludeSessionIds == null || excludeSessionIds.isEmpty()) {
            return;
        }

        Set<String> distinctIds = new HashSet<>(excludeSessionIds);
        long ownedCount = quizSessionRepository.countByIdInAndUserId(distinctIds, userId);
        if (ownedCount != distinctIds.size()) {
            throw new IllegalArgumentException("제외할 퀴즈 세션을 찾을 수 없거나 접근 권한이 없습니다");
        }
    }

    private void validateAiResponse(AiClient.QuizGenerateAiResponse response) {
        if (response == null || isBlank(response.getCourseId()) || response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new RuntimeException("AI 퀴즈 생성 응답이 올바르지 않습니다");
        }

        for (AiClient.QuizGenerateAiResponse.QuestionDto question : response.getQuestions()) {
            if (question == null || isBlank(question.getType()) || isBlank(question.getQuestion())) {
                throw new RuntimeException("AI 퀴즈 문항 응답이 올바르지 않습니다");
            }

            QuizQuestionType type;
            try {
                type = QuizQuestionType.valueOf(question.getType());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("지원하지 않는 퀴즈 유형입니다: " + question.getType(), e);
            }

            if (type == QuizQuestionType.MULTIPLE_CHOICE) {
                if (question.getOptions() == null || question.getOptions().isEmpty()) {
                    throw new RuntimeException("객관식 문항에는 선택지가 필요합니다");
                }

                boolean invalidOption = question.getOptions().stream()
                        .anyMatch(option -> option == null || isBlank(option.getId()) || isBlank(option.getText()));
                if (invalidOption) {
                    throw new RuntimeException("AI 퀴즈 선택지 응답이 올바르지 않습니다");
                }
            }

            if (type != QuizQuestionType.MULTIPLE_CHOICE && question.getOptions() != null) {
                question.setOptions(null);
            }
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}

package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.quiz.request.QuizSubmitRequest;
import com.example.kuriq.dto.quiz.response.QuizHistoryResponse;
import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.dto.quiz.response.QuizSubmitResponse;
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
import java.util.Map;
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
                    .correctAnswer(q.getCorrectAnswer())
                    .explanation(q.getExplanation())
                    .noteReference(q.getNoteReference())
                    .weakTopic(q.getWeakTopic())
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

    public QuizSubmitResponse submit(String quizSessionId, QuizSubmitRequest request, String userId) {
        QuizSession session = quizSessionRepository.findById(quizSessionId)
                .orElseThrow(() -> new IllegalArgumentException("퀴즈 세션을 찾을 수 없습니다"));
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new IllegalArgumentException("퀴즈 세션에 접근 권한이 없습니다");
        }

        Map<String, QuizQuestion> questionMap = session.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
        Set<String> submittedIds = new HashSet<>();
        List<QuizSubmitResponse.ResultDto> results = new java.util.ArrayList<>();
        Set<String> weakTopics = new java.util.LinkedHashSet<>();
        int correctCount = 0;

        for (QuizSubmitRequest.AnswerDto answerDto : request.getAnswers()) {
            if (!submittedIds.add(answerDto.getQuestionId())) {
                throw new IllegalArgumentException("중복된 questionId가 있습니다");
            }
            QuizQuestion question = questionMap.get(answerDto.getQuestionId());
            if (question == null) {
                throw new IllegalArgumentException("세션에 포함되지 않은 questionId가 있습니다");
            }

            var evaluated = evaluate(question, answerDto.getAnswer());
            if (Boolean.TRUE.equals(evaluated.getIsCorrect())) correctCount++;
            if (evaluated.getWeakTopic() != null) weakTopics.add(evaluated.getWeakTopic());
            results.add(evaluated);
        }

        if (submittedIds.size() != questionMap.size()) {
            throw new IllegalArgumentException("모든 문항의 답안을 제출해 주세요");
        }

        session.submitScore(correctCount);
        quizSessionRepository.save(session);

        int scorePercent = calculateScorePercent(correctCount, session.getTotalQuestions());
        return QuizSubmitResponse.builder()
                .quizSessionId(session.getId())
                .totalQuestions(session.getTotalQuestions())
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .results(results)
                .quriMessage(scorePercent >= 80 ? "잘했어요!" : "다시 복습해봐요.")
                .weakTopics(new java.util.ArrayList<>(weakTopics))
                .build();
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
            if (isBlank(question.getCorrectAnswer())) {
                throw new RuntimeException("AI 퀴즈 정답 응답이 올바르지 않습니다");
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

                boolean hasCorrectOption = question.getOptions().stream()
                        .anyMatch(option -> normalize(option.getId()).equals(normalize(question.getCorrectAnswer())));
                if (!hasCorrectOption) {
                    throw new RuntimeException("객관식 정답이 선택지에 없습니다");
                }
            }

            if (type == QuizQuestionType.TRUE_FALSE) {
                String correct = normalizeBoolean(question.getCorrectAnswer());
                if (!"true".equals(correct) && !"false".equals(correct)) {
                    throw new RuntimeException("True/False 정답은 true 또는 false여야 합니다");
                }
                question.setCorrectAnswer(correct);
            }

            if (type != QuizQuestionType.MULTIPLE_CHOICE && question.getOptions() != null) {
                question.setOptions(null);
            }
        }
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private QuizSubmitResponse.ResultDto evaluate(QuizQuestion question, Object answer) {
        String type = question.getType().name();
        String correct = question.getCorrectAnswer();
        boolean isCorrect;
        String result = "WRONG";
        String feedback = null;
        String weakTopic = null;

        if (question.getType() == QuizQuestionType.MULTIPLE_CHOICE) {
            if (answer != null && !(answer instanceof String)) throw new IllegalArgumentException("객관식 답안은 문자열이어야 합니다");
            String user = answer == null ? null : String.valueOf(answer);
            isCorrect = normalize(user).equals(normalize(correct));
        } else if (question.getType() == QuizQuestionType.TRUE_FALSE) {
            if (answer != null && !(answer instanceof Boolean) && !(answer instanceof String)) throw new IllegalArgumentException("True/False 답안은 boolean 또는 문자열이어야 합니다");
            String user = normalizeBoolean(answer);
            isCorrect = normalize(user).equals(normalizeBoolean(correct));
        } else {
            if (answer != null && !(answer instanceof String)) throw new IllegalArgumentException("단답형 답안은 문자열이어야 합니다");
            String user = answer == null ? null : String.valueOf(answer);
            isCorrect = normalize(user).equalsIgnoreCase(normalize(correct));
            if (!isCorrect && "배열".equals(normalize(user)) && "리스트".equals(normalize(correct))) {
                result = "PARTIAL";
                feedback = "'배열'은 유사한 개념이지만 정답은 '리스트'입니다.";
                weakTopic = question.getWeakTopic();
            }
        }

        if (isCorrect) result = "CORRECT";
        if (!isCorrect && weakTopic == null) weakTopic = question.getWeakTopic();

        return QuizSubmitResponse.ResultDto.builder()
                .questionId(question.getId())
                .type(type)
                .isCorrect(isCorrect)
                .result(result)
                .userAnswer(answer)
                .correctAnswer(correct)
                .explanation(question.getExplanation())
                .feedback(feedback)
                .noteReference(question.getNoteReference())
                .weakTopic(weakTopic)
                .build();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeBoolean(Object value) {
        if (value instanceof Boolean b) return String.valueOf(b);
        return value == null ? null : String.valueOf(value).trim().toLowerCase();
    }

    private int calculateScorePercent(int correctCount, Integer totalQuestions) {
        return totalQuestions == null || totalQuestions == 0 ? 0 : (correctCount * 100) / totalQuestions;
    }
}

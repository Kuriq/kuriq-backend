package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.quiz.request.QuizSubmitRequest;
import com.example.kuriq.dto.quiz.response.QuizHistoryResponse;
import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.dto.quiz.response.QuizSubmitResponse;
import com.example.kuriq.entity.quiz.QuizResult;
import com.example.kuriq.entity.quiz.QuizOption;
import com.example.kuriq.entity.quiz.QuizQuestion;
import com.example.kuriq.entity.quiz.QuizQuestionType;
import com.example.kuriq.entity.quiz.QuizSession;
import com.example.kuriq.exception.ApiException;
import com.example.kuriq.repository.quiz.QuizResultRepository;
import com.example.kuriq.repository.quiz.QuizSessionRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizResultRepository quizResultRepository;
    private final CourseRepository courseRepository;
    private final com.example.kuriq.repository.note.LearningNoteRepository learningNoteRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public QuizGenerateResponse generate(QuizGenerateRequest request, String userId) {
        validateExcludedSessions(request.getExcludeSessionIds(), userId);

        var note = learningNoteRepository.findById(request.getNoteId())
                .orElseThrow(() -> new ApiException("NOTE_NOT_FOUND", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 노트에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        AiClient.QuizGenerateAiResponse aiResponse = buildQuizResponseFromNote(note);
        validateAiResponse(aiResponse);

        QuizSession session = QuizSession.builder()
                .userId(userId)
                .courseId(note.getCourse().getId())
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
                    .acceptableKeywords(q.getAcceptableKeywords())
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
                .orElseThrow(() -> new ApiException("QUIZ_SESSION_NOT_FOUND", "퀴즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 퀴즈에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
        return quizResultRepository.findBySessionId(quizSessionId)
                .map(r -> toResponse(r, session))
                .orElseGet(() -> gradeAndSave(session, request, userId));
    }

    private QuizSubmitResponse gradeAndSave(QuizSession session, QuizSubmitRequest request, String userId) {

        Map<String, QuizQuestion> questionMap = session.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
        if (request.getAnswers() == null || request.getAnswers().size() != questionMap.size()) {
            throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
        }
        Set<String> submittedIds = new HashSet<>();
        Map<String, Object> answersByQuestionId = new HashMap<>();
        List<QuizSubmitResponse.ResultDto> results = new java.util.ArrayList<>();
        Set<String> weakTopics = new java.util.LinkedHashSet<>();
        int correctCount = 0;

        for (QuizSubmitRequest.AnswerDto answerDto : request.getAnswers()) {
            if (!submittedIds.add(answerDto.getQuestionId())) {
                throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
            }
            QuizQuestion question = questionMap.get(answerDto.getQuestionId());
            if (question == null) {
                throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
            }
            answersByQuestionId.put(answerDto.getQuestionId(), answerDto.getAnswer());
        }

        for (QuizQuestion question : session.getQuestions()) {
            var evaluated = evaluate(question, answersByQuestionId.get(question.getId()), userId);
            if (Boolean.TRUE.equals(evaluated.getIsCorrect())) correctCount++;
            if (evaluated.getWeakTopic() != null) weakTopics.add(evaluated.getWeakTopic());
            results.add(evaluated);
        }

        session.submitScore(correctCount);
        quizSessionRepository.save(session);

        int scorePercent = calculateScorePercent(correctCount, session.getTotalQuestions());
        var response = QuizSubmitResponse.builder()
                .quizSessionId(session.getId())
                .totalQuestions(session.getTotalQuestions())
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .results(results)
                .quriMessage(buildQuriMessage(scorePercent, correctCount, session.getTotalQuestions(), new java.util.ArrayList<>(weakTopics)))
                .weakTopics(new java.util.ArrayList<>(weakTopics))
                .build();

        quizResultRepository.save(QuizResult.builder()
                .sessionId(session.getId())
                .userId(userId)
                .totalQuestions(session.getTotalQuestions())
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .answersJson(writeJson(results))
                .weakTopicsJson(writeJson(new ArrayList<>(weakTopics)))
                .build());
        return response;
    }

    private AiClient.QuizGenerateAiResponse buildQuizResponseFromNote(com.example.kuriq.entity.note.LearningNote note) {
        AiClient.QuizGenerateAiResponse response = new AiClient.QuizGenerateAiResponse();
        response.setCourseId(note.getCourse().getId());

        String content = note.getContent() == null ? "" : note.getContent().trim();
        String topic = extractTopic(content, note.getCourse().getTitle());
        String detail = extractDetail(content, topic);
        String noteReference = buildNoteReference(content);

        AiClient.QuizGenerateAiResponse.QuestionDto q1 = new AiClient.QuizGenerateAiResponse.QuestionDto();
        q1.setQuestionId(java.util.UUID.nameUUIDFromBytes((note.getId() + ":q1").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        q1.setType("MULTIPLE_CHOICE");
        q1.setQuestion("노트에서 정리한 '" + topic + "'의 설명으로 가장 알맞은 것은?");
        q1.setOptions(List.of(
                createOption("A", detail),
                createOption("B", "노트에 적히지 않은 설명"),
                createOption("C", "정반대의 내용"),
                createOption("D", "문맥과 무관한 설명")
        ));
        q1.setCorrectAnswer("A");
        q1.setExplanation("노트에 '" + topic + "'를 " + detail + "로 정리하셨습니다.");
        q1.setNoteReference(noteReference);
        q1.setWeakTopic(topic);

        AiClient.QuizGenerateAiResponse.QuestionDto q2 = new AiClient.QuizGenerateAiResponse.QuestionDto();
        q2.setQuestionId(java.util.UUID.nameUUIDFromBytes((note.getId() + ":q2").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        q2.setType("TRUE_FALSE");
        q2.setQuestion("노트에는 '" + topic + "'에 대해 '" + detail + "'라고 정리되어 있다.");
        q2.setCorrectAnswer("true");
        q2.setExplanation("노트의 핵심 설명과 일치합니다.");
        q2.setNoteReference(noteReference);
        q2.setWeakTopic(topic);

        AiClient.QuizGenerateAiResponse.QuestionDto q3 = new AiClient.QuizGenerateAiResponse.QuestionDto();
        q3.setQuestionId(java.util.UUID.nameUUIDFromBytes((note.getId() + ":q3").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
        q3.setType("SHORT_ANSWER");
        q3.setQuestion("노트에서 핵심 개념으로 언급된 용어는?");
        q3.setCorrectAnswer(topic);
        q3.setExplanation("노트의 핵심 개념을 정확히 기억하면 됩니다.");
        q3.setNoteReference(noteReference);
        q3.setWeakTopic(topic);
        q3.setAcceptableKeywords(buildAcceptableKeywords(topic, content));

        response.setQuestions(List.of(q1, q2, q3));
        return response;
    }

    private String extractTopic(String content, String courseTitle) {
        if (content == null || content.isBlank()) {
            return courseTitle == null || courseTitle.isBlank() ? "핵심 개념" : courseTitle;
        }
        String firstLine = content.split("[\\n\\r]")[0].trim();
        int colonIndex = firstLine.indexOf(':');
        if (colonIndex > 0) {
            return firstLine.substring(0, colonIndex).trim();
        }
        String[] words = firstLine.split("\\s+");
        if (words.length >= 2) {
            return words[0] + " " + words[1];
        }
        return firstLine.length() > 12 ? firstLine.substring(0, 12).trim() : firstLine;
    }

    private String extractDetail(String content, String topic) {
        if (content == null || content.isBlank()) {
            return topic + "에 대한 설명";
        }
        String firstLine = content.split("[\\n\\r]")[0].trim();
        int colonIndex = firstLine.indexOf(':');
        if (colonIndex > 0 && colonIndex + 1 < firstLine.length()) {
            return firstLine.substring(colonIndex + 1).trim();
        }
        return firstLine;
    }

    private String buildNoteReference(String content) {
        if (content == null || content.isBlank()) {
            return "노트 내용";
        }
        String firstLine = content.split("[\\n\\r]")[0].trim();
        return firstLine.length() > 120 ? firstLine.substring(0, 120).trim() : firstLine;
    }

    private List<String> buildAcceptableKeywords(String topic, String content) {
        java.util.LinkedHashSet<String> keywords = new java.util.LinkedHashSet<>();
        if (topic != null && !topic.isBlank()) {
            keywords.add(topic.trim());
            for (String word : topic.trim().split("\\s+")) {
                if (!word.isBlank()) keywords.add(word.trim());
            }
        }
        if (content != null && !content.isBlank()) {
            String normalized = content.replace(':', ' ').replace('-', ' ');
            for (String token : normalized.split("\\s+")) {
                String cleaned = token.replaceAll("[\\[\\]\\(\\)\\{\\},。.!?]", "").trim();
                if (cleaned.length() >= 2) {
                    keywords.add(cleaned);
                }
            }
        }
        return List.copyOf(keywords);
    }


    private AiClient.QuizGenerateAiResponse.OptionDto createOption(String id, String text) {
        AiClient.QuizGenerateAiResponse.OptionDto option = new AiClient.QuizGenerateAiResponse.OptionDto();
        option.setId(id);
        option.setText(text);
        return option;
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

    private QuizSubmitResponse.ResultDto evaluate(QuizQuestion question, Object answer, String userId) {
        String type = question.getType().name();
        String correct = question.getCorrectAnswer();
        boolean isCorrect;
        String result = "WRONG";
        String feedback = null;
        String weakTopic = null;

        if (question.getType() == QuizQuestionType.MULTIPLE_CHOICE) {
            if (answer != null && !(answer instanceof String)) throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
            String user = answer == null ? null : String.valueOf(answer);
            isCorrect = normalize(user).equals(normalize(correct));
        } else if (question.getType() == QuizQuestionType.TRUE_FALSE) {
            if (answer != null && !(answer instanceof Boolean) && !(answer instanceof String)) throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
            String user = normalizeBoolean(answer);
            isCorrect = normalize(user).equals(normalizeBoolean(correct));
        } else {
            if (answer != null && !(answer instanceof String)) throw new ApiException("INVALID_INPUT", "모든 문제에 답변해 주세요.", HttpStatus.BAD_REQUEST);
            String user = answer == null ? null : String.valueOf(answer);
            if (normalize(user).equalsIgnoreCase(normalize(correct))) {
                isCorrect = true;
            } else if (matchesKeywords(user, question.getAcceptableKeywords())) {
                isCorrect = true;
            } else {
                try {
                    var graded = aiClient.gradeShortAnswer(AiClient.QuizGradeAiRequest.builder().question(question.getQuestion()).correctAnswer(question.getCorrectAnswer()).acceptableKeywords(question.getAcceptableKeywords()).userAnswer(user).userId(userId).build());
                    if (graded == null) {
                        result = "GRADING_FAILED";
                        feedback = "채점에 일시적인 문제가 발생했어요. 모범 답안을 확인해 주세요.";
                    } else {
                        String aiResult = normalize(graded.getResult()).toUpperCase();
                        if (Set.of("CORRECT", "PARTIAL", "WRONG").contains(aiResult)) {
                            result = aiResult;
                            feedback = graded.getFeedback();
                        } else {
                            result = "GRADING_FAILED";
                            feedback = "채점에 일시적인 문제가 발생했어요. 모범 답안을 확인해 주세요.";
                        }
                    }
                } catch (Exception e) {
                    result = "GRADING_FAILED";
                    feedback = "채점에 일시적인 문제가 발생했어요. 모범 답안을 확인해 주세요.";
                }
                isCorrect = "CORRECT".equals(result);
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
                .correctAnswer(formatCorrectAnswer(question))
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

    private Object formatCorrectAnswer(QuizQuestion question) {
        if (question.getType() == QuizQuestionType.TRUE_FALSE) {
            return Boolean.parseBoolean(normalizeBoolean(question.getCorrectAnswer()));
        }
        return question.getCorrectAnswer();
    }

    private String buildQuriMessage(int scorePercent, int correctCount, Integer totalQuestions, List<String> weakTopics) {
        if (scorePercent == 100) {
            return "완벽해요! 🎉 노트 정리를 정말 잘 하셨네요!";
        }
        if (scorePercent >= 80) {
            String topic = weakTopics == null || weakTopics.isEmpty() ? "헷갈린" : weakTopics.get(0);
            int total = totalQuestions == null ? 0 : totalQuestions;
            return total + "문제 중 " + correctCount + "개를 맞혔어요! '" + topic + "' 부분을 노트에서 한 번 더 확인해 보세요.";
        }
        if (scorePercent >= 60) {
            return "절반 이상 맞혔어요! 노트를 보충하고 다시 도전해 볼까요?";
        }
        if (scorePercent >= 40) {
            return "조금 어려웠나요? 노트를 다시 읽어보고 한 번 더 풀어봐요!";
        }
        return "괜찮아요! 노트를 보충하면 다음에는 더 잘 할 수 있어요 💪";
    }

    private boolean matchesKeywords(String user, List<String> keywords) {
        if (user == null || keywords == null) return false;
        String normalizedUser = normalize(user).toLowerCase();
        return keywords.stream().filter(Objects::nonNull).map(String::trim).map(String::toLowerCase).anyMatch(normalizedUser::equals);
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private QuizSubmitResponse toResponse(QuizResult result, QuizSession session) {
        try {
            return QuizSubmitResponse.builder()
                    .quizSessionId(session.getId())
                    .totalQuestions(result.getTotalQuestions())
                    .correctCount(result.getCorrectCount())
                    .scorePercent(result.getScorePercent())
                    .results(objectMapper.readValue(result.getAnswersJson(), objectMapper.getTypeFactory().constructCollectionType(List.class, QuizSubmitResponse.ResultDto.class)))
                    .quriMessage(buildQuriMessage(result.getScorePercent(), result.getCorrectCount(), result.getTotalQuestions(), readWeakTopics(result.getWeakTopicsJson())))
                    .weakTopics(readWeakTopics(result.getWeakTopicsJson()))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> readWeakTopics(String weakTopicsJson) throws java.io.IOException {
        if (weakTopicsJson == null || weakTopicsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(weakTopicsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}

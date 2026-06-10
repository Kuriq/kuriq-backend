package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.note.request.NoteCreateRequest;
import com.example.kuriq.dto.note.request.NoteSaveRequest;
import com.example.kuriq.dto.note.response.AiOrganizeResponse;
import com.example.kuriq.dto.note.response.NoteCreateResponse;
import com.example.kuriq.dto.note.response.NoteDetailResponse;
import com.example.kuriq.dto.note.response.NoteSaveResponse;
import com.example.kuriq.entity.note.LearningNote;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.exception.ApiException;
import com.example.kuriq.repository.chat.ChatMessageRepository;
import com.example.kuriq.repository.note.LearningNoteRepository;
import com.example.kuriq.repository.quiz.QuizSessionRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final LearningNoteRepository learningNoteRepository;
    private final CourseRepository courseRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final AiClient aiClient;

    public NoteCreateResponse create(NoteCreateRequest request, String userId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ApiException("COURSE_NOT_FOUND", "강좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        LearningNote note = learningNoteRepository.save(LearningNote.builder()
                .userId(userId)
                .course(course)
                .content(request.getContent())
                .build());

        return NoteCreateResponse.from(note);
    }

    @Transactional(readOnly = true)
    public NoteDetailResponse getNoteByCourseId(String courseId, String userId) {
        LearningNote note = learningNoteRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ApiException("NOTE_NOT_FOUND", "이 강좌의 노트가 아직 없습니다.", HttpStatus.NOT_FOUND));
        return NoteDetailResponse.from(note);
    }

    public NoteSaveResponse saveNote(String noteId, NoteSaveRequest request, String userId) {
        LearningNote note = learningNoteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException("NOTE_NOT_FOUND", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 노트에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        note.updateContent(request.getContent());
        return NoteSaveResponse.from(note);
    }

    @Transactional(readOnly = true)
    public AiOrganizeResponse aiOrganize(String noteId, String userId) {
        LearningNote note = learningNoteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException("NOTE_NOT_FOUND", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 노트에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        System.out.println("=== Note ID: " + noteId);
        System.out.println("=== Note Content Length: " + (note.getContent() != null ? note.getContent().length() : "null"));
        System.out.println("=== Note Content: " + (note.getContent() != null ? note.getContent().substring(0, Math.min(100, note.getContent().length())) : "null"));

        if (note.getContent() == null || note.getContent().trim().length() < 50) {
            throw new ApiException("NOTE_CONTENT_TOO_SHORT", "AI 정리는 50 자 이상 작성 후 사용해주세요.", HttpStatus.BAD_REQUEST);
        }

        AiClient.OrganizeAiRequest request = AiClient.OrganizeAiRequest.builder()
                .noteContent(note.getContent())
                .courseTitle(note.getCourse().getTitle())
                .courseCategory(note.getCourse().getCategory() == null || note.getCourse().getCategory().isBlank() ? "기타" : note.getCourse().getCategory())
                .userId(userId)
                .build();

        System.out.println("=== AI Request: " + request);

        AiClient.OrganizeAiResponse aiResponse;
        try {
            aiResponse = aiClient.organize(request);
        } catch (Exception e) {
            System.out.println("=== AI Error: " + e.getMessage());
            throw new ApiException("AI_ORGANIZE_FAILED", "AI 노트 정리에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        return AiOrganizeResponse.from(aiResponse);
    }

    public void deleteNote(String noteId, String userId) {
        LearningNote note = learningNoteRepository.findById(noteId)
                .orElseThrow(() -> new ApiException("NOTE_NOT_FOUND", "노트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 노트에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 관련 AI 채팅 이력 삭제
        chatMessageRepository.deleteByUserIdAndNoteId(userId, noteId);

        // 관련 퀴즈 세션 삭제 (QuizQuestion은 cascade로 함께 삭제됨)
        quizSessionRepository.deleteByUserIdAndNoteId(userId, noteId);

        // 노트 삭제
        learningNoteRepository.delete(note);
    }
}

package com.example.kuriq.service;

import com.example.kuriq.dto.note.request.NoteCreateRequest;
import com.example.kuriq.dto.note.response.NoteCreateResponse;
import com.example.kuriq.entity.note.LearningNote;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.exception.ApiException;
import com.example.kuriq.repository.note.LearningNoteRepository;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteService {

    private final LearningNoteRepository learningNoteRepository;
    private final CourseRepository courseRepository;

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
}

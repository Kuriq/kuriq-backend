package com.example.kuriq.dto.note.response;

import com.example.kuriq.entity.note.LearningNote;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Builder
public class NoteCreateResponse {
    private String noteId;
    private String courseId;
    private String content;
    private OffsetDateTime lastSavedAt;

    public static NoteCreateResponse from(LearningNote note) {
        return NoteCreateResponse.builder()
                .noteId(note.getId())
                .courseId(note.getCourse().getId())
                .content(note.getContent())
                .lastSavedAt(note.getLastSavedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .build();
    }
}

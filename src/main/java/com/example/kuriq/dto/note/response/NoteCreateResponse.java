package com.example.kuriq.dto.note.response;

import com.example.kuriq.entity.note.LearningNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Builder
@Schema(description = "학습 노트 생성 응답")
public class NoteCreateResponse {
    @Schema(description = "생성된 노트 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String noteId;

    @Schema(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String courseId;

    @Schema(description = "저장된 노트 내용", example = "파이썬 리스트: 순서가 있고 수정 가능한 자료형. append로 값을 추가할 수 있다.")
    private String content;

    @Schema(description = "마지막 저장 시각", example = "2026-04-16T10:00:00+09:00")
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

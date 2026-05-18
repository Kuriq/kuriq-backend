package com.example.kuriq.dto.note.response;

import com.example.kuriq.entity.note.LearningNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Builder
@Schema(description = "학습 노트 저장 응답")
public class NoteSaveResponse {

    @Schema(description = "노트 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String noteId;

    @Schema(description = "마지막 저장 시각", example = "2026-04-16T10:05:00+09:00")
    private OffsetDateTime lastSavedAt;

    @Schema(description = "글자 수", example = "1350")
    private int characterCount;

    public static NoteSaveResponse from(LearningNote note) {
        return NoteSaveResponse.builder()
                .noteId(note.getId())
                .lastSavedAt(note.getLastSavedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .characterCount(note.getContent().length())
                .build();
    }
}

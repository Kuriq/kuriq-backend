package com.example.kuriq.dto.note.response;

import com.example.kuriq.entity.note.LearningNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Builder
@Schema(description = "학습 노트 조회 응답")
public class NoteDetailResponse {

    @Schema(description = "노트 ID", example = "660e8400-e29b-41d4-a716-446655440000")
    private String noteId;

    @Schema(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String courseId;

    @Schema(description = "강좌명", example = "모두를 위한 파이썬")
    private String courseTitle;

    @Schema(description = "플랫폼명", example = "K-MOOC")
    private String platform;

    @Schema(description = "노트 내용", example = "## 1강 - 변수와 자료형\n\n파이썬에서 변수는...")
    private String content;

    @Schema(description = "글자 수", example = "1247")
    private int characterCount;

    @Schema(description = "마지막 저장 시각", example = "2026-04-15T14:30:00+09:00")
    private OffsetDateTime lastSavedAt;

    @Schema(description = "생성 시각", example = "2026-04-10T09:00:00+09:00")
    private OffsetDateTime createdAt;

    public static NoteDetailResponse from(LearningNote note) {
        return NoteDetailResponse.builder()
                .noteId(note.getId())
                .courseId(note.getCourse().getId())
                .courseTitle(note.getCourse().getTitle())
                .platform(note.getCourse().getPlatform())
                .content(note.getContent())
                .characterCount(note.getContent().length())
                .lastSavedAt(note.getLastSavedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .createdAt(note.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .build();
    }
}

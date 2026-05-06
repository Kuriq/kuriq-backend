package com.example.kuriq.dto.note.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class NoteCreateRequest {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "courseId를 입력해 주세요")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "courseId는 UUID 형식이어야 합니다")
    private String courseId;

    @Schema(example = "리스트: 순서 있음, 수정 가능(mutable)")
    @NotBlank(message = "content를 입력해 주세요")
    @Size(min = 1, max = 10000, message = "content는 1~10,000자여야 합니다")
    private String content;
}

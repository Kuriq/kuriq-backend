package com.example.kuriq.dto.note.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "학습 노트 생성 요청")
@Getter
public class NoteCreateRequest {

    @Schema(description = "노트를 작성할 강좌 ID", example = "11111111-1111-1111-1111-111111111111")
    @NotBlank(message = "courseId를 입력해 주세요")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "courseId는 UUID 형식이어야 합니다")
    private String courseId;

    @Schema(description = "학습 노트 내용", example = "파이썬 리스트: 순서가 있고 수정 가능한 자료형. append로 값을 추가할 수 있다.")
    @NotBlank(message = "content를 입력해 주세요")
    @Size(min = 1, max = 10000, message = "content는 1~10,000자여야 합니다")
    private String content;
}

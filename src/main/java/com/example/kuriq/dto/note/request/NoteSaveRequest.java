package com.example.kuriq.dto.note.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "학습 노트 저장 요청")
@Getter
public class NoteSaveRequest {

    @Schema(description = "저장할 노트 내용", example = "## 1강 - 변수와 자료형\n\n파이썬에서 변수는...")
    @NotBlank(message = "content를 입력해 주세요")
    @Size(min = 1, max = 10000, message = "content는 1~10,000자여야 합니다")
    private String content;
}

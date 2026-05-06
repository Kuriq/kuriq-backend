package com.example.kuriq.dto.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ChatSendRequest {

    @Schema(example = "리스트랑 튜플 차이가 뭐야?")
    @NotBlank(message = "message를 입력해 주세요")
    @Size(min = 1, max = 1000, message = "message는 1~1,000자여야 합니다")
    private String message;
}

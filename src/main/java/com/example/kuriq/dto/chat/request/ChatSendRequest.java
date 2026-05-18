package com.example.kuriq.dto.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Schema(description = "노트 기반 AI 채팅 메시지 전송 요청")
@Getter
public class ChatSendRequest {

    @Schema(description = "사용자 메시지", example = "리스트랑 튜플 차이가 뭐야?")
    @NotBlank(message = "message를 입력해 주세요")
    @Size(min = 1, max = 1000, message = "message는 1~1,000자여야 합니다")
    private String message;
}

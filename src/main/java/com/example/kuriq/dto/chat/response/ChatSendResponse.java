package com.example.kuriq.dto.chat.response;

import com.example.kuriq.entity.chat.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
@Schema(description = "노트 기반 AI 채팅 메시지 전송 응답")
public class ChatSendResponse {
    @Schema(description = "채팅 메시지 ID", example = "770e8400-e29b-41d4-a716-446655440000")
    private String chatId;

    @Schema(description = "메시지 역할", example = "assistant")
    private String role;

    @Schema(description = "AI 응답 메시지", example = "노트에 '리스트: 순서 있음, 수정 가능(mutable)'이라고 정리하셨죠! 튜플은 생성 후 수정할 수 없다는 점이 달라요.")
    private String message;

    @Schema(description = "AI 답변에 참조된 노트 문장", example = "[\"리스트: 순서 있음, 수정 가능(mutable)\"]")
    private List<String> noteReferences;

    @Schema(description = "메시지 생성 시각", example = "2026-04-16T14:35:00+09:00")
    private OffsetDateTime timestamp;

    public static ChatSendResponse from(ChatMessage message) {
        return ChatSendResponse.builder()
                .chatId(message.getId())
                .role(message.getRole())
                .message(message.getMessage())
                .noteReferences(message.getNoteReferences())
                .timestamp(message.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                .build();
    }
}

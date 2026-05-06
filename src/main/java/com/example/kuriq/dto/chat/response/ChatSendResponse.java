package com.example.kuriq.dto.chat.response;

import com.example.kuriq.entity.chat.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class ChatSendResponse {
    private String chatId;
    private String role;
    private String message;
    private List<String> noteReferences;
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

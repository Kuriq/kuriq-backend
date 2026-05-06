package com.example.kuriq.dto.chat.response;

import com.example.kuriq.entity.chat.ChatMessage;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class ChatHistoryResponse {

    private List<Item> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
    private boolean hasNext;

    public static ChatHistoryResponse from(Page<ChatMessage> page) {
        return ChatHistoryResponse.builder()
                .content(page.getContent().stream().map(Item::from).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .hasNext(page.hasNext())
                .build();
    }

    @Getter
    @Builder
    public static class Item {
        private String chatId;
        private String role;
        private String message;
        private List<String> noteReferences;
        private OffsetDateTime timestamp;

        public static Item from(ChatMessage message) {
            return Item.builder()
                    .chatId(message.getId())
                    .role(message.getRole())
                    .message(message.getMessage())
                    .noteReferences(message.getNoteReferences())
                    .timestamp(message.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime())
                    .build();
        }
    }
}

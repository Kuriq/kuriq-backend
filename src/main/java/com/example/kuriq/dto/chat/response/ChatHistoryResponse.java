package com.example.kuriq.dto.chat.response;

import com.example.kuriq.entity.chat.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
@Schema(description = "노트 기반 AI 채팅 이력 조회 응답")
public class ChatHistoryResponse {

    @Schema(description = "채팅 메시지 목록")
    private List<Item> content;
    @Schema(description = "전체 메시지 수", example = "12")
    private long totalElements;
    @Schema(description = "전체 페이지 수", example = "1")
    private int totalPages;
    @Schema(description = "현재 페이지", example = "0")
    private int currentPage;
    @Schema(description = "페이지 크기", example = "20")
    private int size;
    @Schema(description = "다음 페이지 존재 여부", example = "false")
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
    @Schema(description = "채팅 이력 항목")
    public static class Item {
        @Schema(description = "채팅 메시지 ID", example = "770e8400-e29b-41d4-a716-446655440000")
        private String chatId;
        @Schema(description = "메시지 역할", example = "user")
        private String role;
        @Schema(description = "메시지 내용", example = "리스트랑 튜플 차이가 뭐야?")
        private String message;
        @Schema(description = "AI 답변에 참조된 노트 문장", example = "[\"리스트: 순서 있음, 수정 가능(mutable)\"]")
        private List<String> noteReferences;
        @Schema(description = "메시지 생성 시각", example = "2026-04-16T14:35:00+09:00")
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

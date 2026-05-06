package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.chat.request.ChatSendRequest;
import com.example.kuriq.dto.chat.response.ChatHistoryResponse;
import com.example.kuriq.dto.chat.response.ChatSendResponse;
import com.example.kuriq.entity.chat.ChatMessage;
import com.example.kuriq.exception.ApiException;
import com.example.kuriq.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;

    public void resetHistory(String noteId, String userId) {
        chatMessageRepository.deleteByUserIdAndNoteId(userId, noteId);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(String noteId, String userId, int page, int size) {
        return ChatHistoryResponse.from(
                chatMessageRepository.findByUserIdAndNoteId(
                        userId,
                        noteId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
                )
        );
    }

    public ChatSendResponse sendMessage(String noteId, String userId, ChatSendRequest request) {
        List<AiClient.ChatAiRequest.ChatHistoryItem> recentHistory = chatMessageRepository
                .findTop20ByUserIdAndNoteIdOrderByCreatedAtDesc(userId, noteId)
                .stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(chat -> AiClient.ChatAiRequest.ChatHistoryItem.builder()
                        .role(chat.getRole())
                        .message(chat.getMessage())
                        .build())
                .toList();

        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .noteId(noteId)
                .role("user")
                .message(request.getMessage())
                .noteReferences(List.of())
                .build());

        AiClient.ChatAiResponse aiResponse;
        try {
            aiResponse = aiClient.chat(AiClient.ChatAiRequest.builder()
                    .noteId(noteId)
                    .noteContent("")
                    .courseMetadata("")
                    .recentHistory(recentHistory)
                    .message(userMessage.getMessage())
                    .userId(userId)
                    .build());
        } catch (Exception e) {
            throw new ApiException("AI_CHAT_FAILED", "AI 채팅 응답 생성에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        if (aiResponse == null || aiResponse.getMessage() == null || aiResponse.getMessage().isBlank()) {
            throw new ApiException("AI_CHAT_FAILED", "AI 채팅 응답 생성에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        }

        ChatMessage assistantMessage = chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .noteId(noteId)
                .role("assistant")
                .message(aiResponse.getMessage())
                .noteReferences(aiResponse.getNoteReferences() == null ? Collections.emptyList() : aiResponse.getNoteReferences())
                .build());

        return ChatSendResponse.from(assistantMessage);
    }
}

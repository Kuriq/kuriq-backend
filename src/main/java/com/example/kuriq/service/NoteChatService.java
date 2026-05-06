package com.example.kuriq.service;

import com.example.kuriq.dto.chat.response.ChatHistoryResponse;
import com.example.kuriq.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteChatService {

    private final ChatMessageRepository chatMessageRepository;

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
}

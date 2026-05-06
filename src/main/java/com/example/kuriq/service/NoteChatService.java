package com.example.kuriq.service;

import com.example.kuriq.repository.chat.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
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
}

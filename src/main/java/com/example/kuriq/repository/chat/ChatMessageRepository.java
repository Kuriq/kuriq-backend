package com.example.kuriq.repository.chat;

import com.example.kuriq.entity.chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    void deleteByUserIdAndNoteId(String userId, String noteId);

    Page<ChatMessage> findByUserIdAndNoteId(String userId, String noteId, Pageable pageable);
}

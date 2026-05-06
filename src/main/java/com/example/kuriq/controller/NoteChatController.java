package com.example.kuriq.controller;

import com.example.kuriq.dto.chat.response.ChatHistoryResponse;
import com.example.kuriq.service.NoteChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Note Chat", description = "노트 기반 AI 채팅 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes/{noteId}/chat")
@RequiredArgsConstructor
public class NoteChatController {

    private final NoteChatService noteChatService;

    @Operation(summary = "대화 초기화")
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> resetHistory(@PathVariable String noteId,
                                                            @AuthenticationPrincipal String userId) {
        noteChatService.resetHistory(noteId, userId);
        return ResponseEntity.ok(Map.of("message", "대화가 초기화되었습니다."));
    }

    @Operation(summary = "대화 이력 조회")
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getHistory(@PathVariable String noteId,
                                                          @AuthenticationPrincipal String userId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(noteChatService.getHistory(noteId, userId, page, size));
    }
}

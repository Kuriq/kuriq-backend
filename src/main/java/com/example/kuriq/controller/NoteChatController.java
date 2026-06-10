package com.example.kuriq.controller;

import com.example.kuriq.dto.chat.request.ChatSendRequest;
import com.example.kuriq.dto.chat.response.ChatHistoryResponse;
import com.example.kuriq.dto.chat.response.ChatSendResponse;
import com.example.kuriq.service.NoteChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Note Chat", description = "노트 기반 AI 채팅 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes/{noteId}/chat")
@RequiredArgsConstructor
@Validated
public class NoteChatController {

    private static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final NoteChatService noteChatService;

    @Operation(summary = "대화 초기화", description = "해당 노트의 AI 채팅 대화 이력을 모두 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 초기화 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ \"message\": \"대화가 초기화되었습니다.\" }"))),
            @ApiResponse(responseCode = "400", description = "noteId 형식 오류")
    })
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> resetHistory(@Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
                                                            @PathVariable @Pattern(regexp = UUID_PATTERN, message = "noteId는 UUID 형식이어야 합니다") String noteId,
                                                            @AuthenticationPrincipal String userId) {
        noteChatService.resetHistory(noteId, userId);
        return ResponseEntity.ok(Map.of("message", "대화가 초기화되었습니다."));
    }

    @Operation(summary = "대화 이력 조회", description = "해당 노트의 AI 채팅 이력을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 이력 조회 성공"),
            @ApiResponse(responseCode = "400", description = "noteId 또는 페이지 파라미터 검증 실패")
    })
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getHistory(@Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
                                                          @PathVariable @Pattern(regexp = UUID_PATTERN, message = "noteId는 UUID 형식이어야 합니다") String noteId,
                                                          @AuthenticationPrincipal String userId,
                                                          @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(noteChatService.getHistory(noteId, userId, page, size));
    }

    @Operation(
            summary = "메시지 전송",
            description = "사용자 메시지와 최근 대화 이력을 FastAPI /internal/ai/chat으로 전달하고, AI 답변을 저장한 뒤 반환합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "채팅 메시지 전송 요청 예시",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "message": "리스트랑 튜플 차이가 뭐야?"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 답변 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패"),
            @ApiResponse(responseCode = "502", description = "AI 채팅 응답 생성 실패")
    })
    @PostMapping
    public ResponseEntity<ChatSendResponse> sendMessage(@Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
                                                        @PathVariable @Pattern(regexp = UUID_PATTERN, message = "noteId는 UUID 형식이어야 합니다") String noteId,
                                                        @AuthenticationPrincipal String userId,
                                                        @Valid @RequestBody ChatSendRequest request) {
        return ResponseEntity.ok(noteChatService.sendMessage(noteId, userId, request));
    }
}

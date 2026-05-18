package com.example.kuriq.controller;

import com.example.kuriq.dto.note.request.NoteCreateRequest;
import com.example.kuriq.dto.note.request.NoteSaveRequest;
import com.example.kuriq.dto.note.response.AiOrganizeResponse;
import com.example.kuriq.dto.note.response.NoteCreateResponse;
import com.example.kuriq.dto.note.response.NoteDetailResponse;
import com.example.kuriq.dto.note.response.NoteSaveResponse;
import com.example.kuriq.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Note", description = "학습 노트 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(
            summary = "노트 생성",
            description = "강좌를 기준으로 사용자의 학습 노트를 생성합니다. content는 1~10,000자까지 저장할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "노트 생성 요청 예시",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "courseId": "11111111-1111-1111-1111-111111111111",
                              "content": "파이썬 리스트: 순서가 있고 수정 가능한 자료형. append로 값을 추가할 수 있다."
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "노트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "강좌를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<NoteCreateResponse> create(@Valid @RequestBody NoteCreateRequest request,
                                                     @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(request, userId));
    }

    @Operation(
            summary = "노트 조회 (강좌별)",
            description = "특정 강좌에 대한 사용자의 학습 노트를 조회합니다. 노트가 없으면 404를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "이 강좌의 노트가 아직 없음")
    })
    @GetMapping
    public ResponseEntity<NoteDetailResponse> getByCourse(
            @Parameter(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String courseId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(noteService.getNoteByCourseId(courseId, userId));
    }

    @Operation(
            summary = "노트 저장 (자동 저장)",
            description = "학습 노트 내용을 저장합니다. 프론트엔드에서 마지막 입력 후 3초 경과 시 자동 호출됩니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "노트 저장 요청 예시",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "content": "## 1강 - 변수와 자료형\n\n파이썬에서 변수는..."
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 저장 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "노트 접근 권한 없음")
    })
    @PutMapping("/{noteId}")
    public ResponseEntity<NoteSaveResponse> save(
            @Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String noteId,
            @Valid @RequestBody NoteSaveRequest request,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(noteService.saveNote(noteId, request, userId));
    }

    @Operation(
            summary = "AI 노트 정리",
            description = "노트 내용을 AI 마이크로서비스에 전달하여 LLM 분석을 수행합니다. 키워드 추출, 구조화된 요약, 학습 제안을 반환하며 DB에 저장하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 노트 정리 성공"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "노트 접근 권한 없음"),
            @ApiResponse(responseCode = "502", description = "AI 노트 정리 실패")
    })
    @PostMapping("/{noteId}/ai-organize")
    public ResponseEntity<AiOrganizeResponse> aiOrganize(
            @Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String noteId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(noteService.aiOrganize(noteId, userId));
    }

    @Operation(
            summary = "노트 삭제",
            description = "학습 노트를 삭제합니다. 관련 퀴즈 기록과 AI 채팅 이력도 함께 삭제됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "노트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "노트를 찾을 수 없음"),
            @ApiResponse(responseCode = "403", description = "노트 접근 권한 없음")
    })
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Map<String, String>> delete(
            @Parameter(description = "노트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String noteId,
            @AuthenticationPrincipal String userId) {
        noteService.deleteNote(noteId, userId);
        return ResponseEntity.ok(Map.of("message", "노트가 삭제되었습니다."));
    }
}

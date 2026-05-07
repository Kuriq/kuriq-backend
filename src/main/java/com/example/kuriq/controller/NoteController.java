package com.example.kuriq.controller;

import com.example.kuriq.dto.note.request.NoteCreateRequest;
import com.example.kuriq.dto.note.response.NoteCreateResponse;
import com.example.kuriq.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

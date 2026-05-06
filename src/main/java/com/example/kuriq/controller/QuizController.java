package com.example.kuriq.controller;

import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.request.QuizSubmitRequest;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.dto.quiz.response.QuizHistoryResponse;
import com.example.kuriq.dto.quiz.response.QuizSubmitResponse;
import com.example.kuriq.service.QuizService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quiz", description = "퀴즈 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(
            summary = "퀴즈 생성",
            description = "노트 ID를 기반으로 AI 퀴즈 세션과 문항을 생성합니다. excludeSessionIds로 이전 세션과의 중복 생성을 줄일 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "퀴즈 생성 요청 예시",
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "noteId": "550e8400-e29b-41d4-a716-446655440000",
                      "excludeSessionIds": ["660e8400-e29b-41d4-a716-446655440000"]
                    }
                    """))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "퀴즈 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패")
    })
    @PostMapping("/generate")
    public ResponseEntity<QuizGenerateResponse> generate(@Valid @RequestBody QuizGenerateRequest request,
                                                         @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.generate(request, userId));
    }

    @Operation(
            summary = "퀴즈 히스토리 조회",
            description = "사용자의 퀴즈 세션 이력을 최신순으로 조회합니다. courseId를 전달하면 특정 강좌로 필터링합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "퀴즈 이력 조회 성공"),
            @ApiResponse(responseCode = "400", description = "페이지 파라미터 오류")
    })
    @GetMapping("/history")
    public ResponseEntity<QuizHistoryResponse> history(
            @AuthenticationPrincipal String userId,
            @Parameter(description = "강좌 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String courseId,
            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quizService.history(userId, courseId, page, size));
    }

    @Operation(
            summary = "퀴즈 제출",
            description = "사용자가 제출한 답안을 채점합니다. 객관식/OX는 Spring에서 직접 채점하고, 단답형은 정확 일치와 허용 키워드 검사 후 필요할 때 FastAPI 단답형 채점을 호출합니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "퀴즈 답안 제출 요청 예시",
            required = true,
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "answers": [
                        { "questionId": "770e8400-e29b-41d4-a716-446655440001", "answer": "B" },
                        { "questionId": "770e8400-e29b-41d4-a716-446655440002", "answer": false },
                        { "questionId": "770e8400-e29b-41d4-a716-446655440003", "answer": "리스트" }
                      ]
                    }
                    """))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채점 성공 또는 기존 제출 결과 반환"),
            @ApiResponse(responseCode = "400", description = "답안 누락/중복/형식 오류"),
            @ApiResponse(responseCode = "403", description = "퀴즈 세션 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "퀴즈 세션을 찾을 수 없음")
    })
    @PostMapping("/{quizSessionId}/submit")
    public ResponseEntity<QuizSubmitResponse> submit(
            @Valid @RequestBody QuizSubmitRequest request,
            @AuthenticationPrincipal String userId,
            @Parameter(description = "퀴즈 세션 ID", example = "660e8400-e29b-41d4-a716-446655440000")
            @PathVariable String quizSessionId) {
        return ResponseEntity.ok(quizService.submit(quizSessionId, request, userId));
    }
}

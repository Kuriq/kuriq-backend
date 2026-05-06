package com.example.kuriq.controller;

import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.request.QuizSubmitRequest;
import com.example.kuriq.dto.quiz.response.QuizHistoryResponse;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.dto.quiz.response.QuizSubmitResponse;
import com.example.kuriq.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quiz", description = "퀴즈 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "퀴즈 생성")
    @PostMapping("/generate")
    public ResponseEntity<QuizGenerateResponse> generate(@Valid @RequestBody QuizGenerateRequest request,
                                                         @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.generate(request, userId));
    }

    @Operation(summary = "퀴즈 히스토리 조회")
    @GetMapping("/history")
    public ResponseEntity<QuizHistoryResponse> history(@AuthenticationPrincipal String userId,
                                                       @RequestParam(required = false) String courseId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quizService.history(userId, courseId, page, size));
    }

    @Operation(summary = "퀴즈 제출")
    @PostMapping("/{quizSessionId}/submit")
    public ResponseEntity<QuizSubmitResponse> submit(@Valid @RequestBody QuizSubmitRequest request,
                                                     @AuthenticationPrincipal String userId,
                                                     @PathVariable String quizSessionId) {
        return ResponseEntity.ok(quizService.submit(quizSessionId, request, userId));
    }
}

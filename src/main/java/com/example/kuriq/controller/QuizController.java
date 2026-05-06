package com.example.kuriq.controller;

import com.example.kuriq.dto.quiz.request.QuizGenerateRequest;
import com.example.kuriq.dto.quiz.response.QuizGenerateResponse;
import com.example.kuriq.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
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
}

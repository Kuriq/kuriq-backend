package com.example.kuriq.controller;

import com.example.kuriq.dto.note.request.NoteCreateRequest;
import com.example.kuriq.dto.note.response.NoteCreateResponse;
import com.example.kuriq.service.NoteService;
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

@Tag(name = "Note", description = "학습 노트 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "노트 생성")
    @PostMapping
    public ResponseEntity<NoteCreateResponse> create(@Valid @RequestBody NoteCreateRequest request,
                                                     @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(request, userId));
    }
}

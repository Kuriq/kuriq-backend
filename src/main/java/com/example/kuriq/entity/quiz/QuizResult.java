package com.example.kuriq.entity.quiz;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QuizResult {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;
    @Column(name = "session_id", nullable = false, length = 36, unique = true)
    private String sessionId;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(nullable = false)
    private Integer totalQuestions;
    @Column(nullable = false)
    private Integer correctCount;
    @Column(nullable = false)
    private Integer scorePercent;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answersJson;
    @Column(columnDefinition = "TEXT")
    private String weakTopicsJson;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}

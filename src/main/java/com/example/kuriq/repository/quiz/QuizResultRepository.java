package com.example.kuriq.repository.quiz;

import com.example.kuriq.entity.quiz.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, String> {
    Optional<QuizResult> findBySessionId(String sessionId);
    boolean existsBySessionId(String sessionId);
}

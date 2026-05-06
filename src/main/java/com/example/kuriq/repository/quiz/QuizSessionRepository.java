package com.example.kuriq.repository.quiz;

import com.example.kuriq.entity.quiz.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface QuizSessionRepository extends JpaRepository<QuizSession, String> {

    long countByIdInAndUserId(Collection<String> ids, String userId);
}

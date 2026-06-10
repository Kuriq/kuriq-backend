package com.example.kuriq.repository.quiz;

import com.example.kuriq.entity.quiz.QuizSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, String> {

    long countByIdInAndUserId(Collection<String> ids, String userId);

    Page<QuizSession> findByUserIdAndCourseId(String userId, String courseId, Pageable pageable);

    Page<QuizSession> findByUserId(String userId, Pageable pageable);

    Optional<QuizSession> findByIdAndUserId(String id, String userId);

    boolean existsByIdAndUserId(String id, String userId);

    void deleteByUserIdAndNoteId(String userId, String noteId);
}

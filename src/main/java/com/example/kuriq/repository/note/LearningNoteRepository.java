package com.example.kuriq.repository.note;

import com.example.kuriq.entity.note.LearningNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningNoteRepository extends JpaRepository<LearningNote, String> {

    Optional<LearningNote> findByUserIdAndCourseId(String userId, String courseId);
}

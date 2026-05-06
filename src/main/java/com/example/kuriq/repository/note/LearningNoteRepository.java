package com.example.kuriq.repository.note;

import com.example.kuriq.entity.note.LearningNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningNoteRepository extends JpaRepository<LearningNote, String> {
}

package com.example.kuriq.entity.quiz;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class QuizQuestion {

    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_session_id", nullable = false)
    private QuizSession quizSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuizQuestionType type;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(length = 1000)
    private String correctAnswer;

    @Column(length = 2000)
    private String explanation;

    @Column(length = 500)
    private String noteReference;

    @Column(length = 500)
    private String weakTopic;

    @Convert(converter = JsonStringListConverter.class)
    @Column(length = 2000)
    private List<String> acceptableKeywords;

    @OneToMany(mappedBy = "quizQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();

    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setOptions(List<QuizOption> options) { this.options = options; }
}

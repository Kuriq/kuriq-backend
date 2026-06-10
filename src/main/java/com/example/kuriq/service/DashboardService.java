package com.example.kuriq.service;

import com.example.kuriq.dto.dashboard.response.WeeklyDashboardResponse;
import com.example.kuriq.entity.note.LearningNote;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.Roadmap;
import com.example.kuriq.entity.roadmap.RoadmapItem;
import com.example.kuriq.entity.roadmap.RoadmapWeek;
import com.example.kuriq.exception.ApiException;
import com.example.kuriq.repository.note.LearningNoteRepository;
import com.example.kuriq.repository.roadmap.RoadmapItemRepository;
import com.example.kuriq.repository.roadmap.RoadmapRepository;
import com.example.kuriq.repository.roadmap.RoadmapWeekRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapWeekRepository roadmapWeekRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final LearningNoteRepository learningNoteRepository;

    public WeeklyDashboardResponse getWeeklyDashboard(String roadmapId, Integer weekNumber, String userId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new ApiException("ROADMAP_NOT_FOUND", "로드맵을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!Objects.equals(roadmap.getUserId(), userId)) {
            throw new ApiException("FORBIDDEN", "이 로드맵에 접근할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        // weekNumber 생략 시 현재 주차 자동 산정
        int targetWeek = (weekNumber != null) ? weekNumber : roadmap.currentWeek();

        // 주차 메타데이터 조회
        RoadmapWeek week = roadmapWeekRepository.findByRoadmapIdAndWeekNumber(roadmapId, targetWeek)
                .orElse(null);
        String weekTitle = (week != null) ? week.getTitle() : targetWeek + "주차";

        // 해당 주차 강좌 목록 조회
        List<RoadmapItem> items = roadmapItemRepository.findByRoadmapIdAndWeekNumberOrderByOrderInWeekAsc(roadmapId, targetWeek);

        // 노트 작성 여부 확인을 위한 courseId 집합
        Set<String> courseIds = items.stream()
                .map(item -> item.getCourse().getId())
                .collect(Collectors.toSet());

        // 사용자의 노트 존재 여부 조회
        Set<String> notedCourseIds = courseIds.stream()
                .filter(courseId -> learningNoteRepository.existsByUserIdAndCourseId(userId, courseId))
                .collect(Collectors.toSet());

        // 강좌 항목 DTO 변환
        List<WeeklyDashboardResponse.CourseItem> courseItems = items.stream()
                .map(item -> {
                    Course course = item.getCourse();
                    return WeeklyDashboardResponse.CourseItem.builder()
                            .itemId(item.getId())
                            .courseId(course.getId())
                            .title(course.getTitle())
                            .platform(course.getPlatform() != null ? course.getPlatform().name() : null)
                            .difficulty(course.getDifficulty())
                            .estimatedHours(course.getEstimatedHours() != null ? course.getEstimatedHours() : BigDecimal.ZERO)
                            .isCompleted(Boolean.TRUE.equals(item.getIsCompleted()))
                            .completedAt(item.getCompletedAt() != null
                                    ? item.getCompletedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime()
                                    : null)
                            .url(course.getUrl())
                            .hasNote(notedCourseIds.contains(course.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        // 통계 계산
        int totalCourses = items.size();
        int completedCourses = (int) items.stream().filter(i -> Boolean.TRUE.equals(i.getIsCompleted())).count();
        int progressPercent = totalCourses > 0 ? (completedCourses * 100) / totalCourses : 0;

        BigDecimal remainingHours = items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsCompleted()))
                .map(i -> i.getCourse().getEstimatedHours() != null ? i.getCourse().getEstimatedHours() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        // 주차 기간 계산 (로드맵 활성화 기준)
        WeeklyDashboardResponse.DateRange dateRange = calculateDateRange(roadmap, targetWeek);

        // 큐릭 메시지 생성
        WeeklyDashboardResponse.KuriqMessage kuriqMessage = generateKuriqMessage(progressPercent, completedCourses, totalCourses);

        return WeeklyDashboardResponse.builder()
                .roadmapId(roadmapId)
                .weekNumber(targetWeek)
                .weekTitle(weekTitle)
                .dateRange(dateRange)
                .totalCourses(totalCourses)
                .completedCourses(completedCourses)
                .progressPercent(progressPercent)
                .remainingHours(remainingHours)
                .courses(courseItems)
                .kuriqMessage(kuriqMessage)
                .build();
    }

    private WeeklyDashboardResponse.DateRange calculateDateRange(Roadmap roadmap, int weekNumber) {
        LocalDateTime activatedAt = roadmap.getActivatedAt();
        if (activatedAt == null) {
            // 활성화 전이면 오늘 기준 주차 계산
            activatedAt = LocalDateTime.now();
        }

        LocalDate startDate = activatedAt.toLocalDate().plusWeeks(weekNumber - 1);
        // 월요일 기준으로 조정
        startDate = startDate.with(java.time.DayOfWeek.MONDAY);
        LocalDate endDate = startDate.plusDays(6);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return WeeklyDashboardResponse.DateRange.builder()
                .start(startDate.format(formatter))
                .end(endDate.format(formatter))
                .build();
    }

    private WeeklyDashboardResponse.KuriqMessage generateKuriqMessage(int progressPercent, int completed, int total) {
        if (total == 0) {
            return WeeklyDashboardResponse.KuriqMessage.builder()
                    .expression("smile")
                    .text("이번 주차 학습 계획을 확인해 보세요!")
                    .build();
        }

        if (progressPercent == 100) {
            return WeeklyDashboardResponse.KuriqMessage.builder()
                    .expression("celebrate")
                    .text("이번 주차 완벽 완료! 정말 대단해요 🎉")
                    .build();
        }

        if (progressPercent >= 75) {
            return WeeklyDashboardResponse.KuriqMessage.builder()
                    .expression("wink")
                    .text("거의 다 왔어요! 조금만 더 힘내세요 💪")
                    .build();
        }

        if (progressPercent >= 50) {
            return WeeklyDashboardResponse.KuriqMessage.builder()
                    .expression("wink")
                    .text("절반 넘었어요! 잘 하고 있어요 🎉")
                    .build();
        }

        if (progressPercent > 0) {
            return WeeklyDashboardResponse.KuriqMessage.builder()
                    .expression("smile")
                    .text("좋은 시작이에요! 꾸준히 계속해 보세요 📚")
                    .build();
        }

        return WeeklyDashboardResponse.KuriqMessage.builder()
                .expression("neutral")
                .text("새로운 주차가 시작됐어요! 첫걸음을 내딛어 볼까요?")
                .build();
    }
}

package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.roadmap.response.RoadmapResponse;
import com.example.kuriq.entity.roadmap.*;
import com.example.kuriq.repository.roadmap.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadmapService {

    private final RoadmapRepository roadmapRepository;
    private final RoadmapItemRepository roadmapItemRepository;
    private final RoadmapWeekRepository roadmapWeekRepository;
    private final CourseRepository courseRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final AiClient aiClient;

    // 로드맵 생성
    public RoadmapResponse generateRoadmap(String prompt, String userId) {

        // AI 호출
        AiClient.RoadmapGenerateAiResponse ai = aiClient.generateRoadmap(
                AiClient.RoadmapGenerateAiRequest.builder()
                        .prompt(prompt)
                        .userId(userId)
                        .build());

        // AI 응답 courseId → 강좌 조회
        List<String> courseIds = ai.getWeeks().stream()
                .flatMap(w -> w.getCourses().stream())
                .map(AiClient.RoadmapGenerateAiResponse.CourseItemDto::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Course> courseMap = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        int totalCourses = ai.getWeeks().stream()
                .mapToInt(w -> w.getCourses().size())
                .sum();

        // 로드맵 저장 (초기 상태: inactive)
        Roadmap roadmap = Roadmap.create(
                userId, prompt, ai.getGoal(),
                ai.getTotalWeeks(), ai.getWeeklyHours(), totalCourses);
        roadmapRepository.save(roadmap);

        // 주차 + 강좌 항목 저장
        for (AiClient.RoadmapGenerateAiResponse.WeekDto weekDto : ai.getWeeks()) {
            RoadmapWeek week = RoadmapWeek.create(
                    roadmap,
                    weekDto.getWeekNumber(),
                    weekDto.getTitle(),
                    weekDto.getDescription(),
                    BigDecimal.valueOf(weekDto.getTotalHours()));
            roadmapWeekRepository.save(week);

            for (AiClient.RoadmapGenerateAiResponse.CourseItemDto itemDto : weekDto.getCourses()) {
                Course course = courseMap.get(itemDto.getCourseId());
                if (course == null) {
                    log.warn("AI courseId 없음: {}", itemDto.getCourseId());
                    continue;
                }
                roadmapItemRepository.save(
                        RoadmapItem.create(roadmap, course, weekDto.getWeekNumber(), itemDto.getOrderInWeek()));
            }
        }

        log.info("로드맵 생성: roadmapId={}, userId={}", roadmap.getId(), userId);
        return buildRoadmapResponse(roadmap);
    }

    // 내 로드맵 목록 조회
    @Transactional(readOnly = true)
    public List<RoadmapResponse> getMyRoadmaps(String userId, Pageable pageable) {
        return roadmapRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .skip((long) pageable.getPageNumber() * pageable.getPageSize())
                .limit(pageable.getPageSize())
                .map(this::buildRoadmapResponse)
                .collect(Collectors.toList());
    }

    // 로드맵 단건 조회
    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmapById(String roadmapId, String userId) {
        return buildRoadmapResponse(getAndValidate(roadmapId, userId));
    }

    // 로드맵 활성화
    public RoadmapResponse activateRoadmap(String roadmapId, String userId) {
        Roadmap roadmap = getAndValidate(roadmapId, userId);
        roadmapRepository.deactivateCurrentRoadmap(userId);
        roadmap.activate();
        log.info("로드맵 활성화: roadmapId={}", roadmapId);
        return buildRoadmapResponse(roadmap);
    }

    // 로드맵 삭제
    public void deleteRoadmap(String roadmapId, String userId) {
        roadmapRepository.delete(getAndValidate(roadmapId, userId));
        log.info("로드맵 삭제: roadmapId={}", roadmapId);
    }

    // 강좌 완료 처리
    public RoadmapResponse.RoadmapItemResponse completeItem(String itemId, String userId) {
        RoadmapItem item = getItemAndValidate(itemId, userId); // 빠져있던 부분

        if (item.getIsCompleted()) {
            throw new IllegalStateException("이미 완료된 강좌입니다");
        }

        item.complete();

        // 학습 이력 저장 (중복 방지)
        if (!learningHistoryRepository.existsByUserIdAndCourseId(userId, item.getCourse().getId())) {
            learningHistoryRepository.save(
                    LearningHistory.create(
                            userId,
                            item.getCourse().getId(),
                            item.getRoadmap().getId(),
                            item.getId()));
        }

        // 전체 완료 체크
        if (item.getRoadmap().allItemsCompleted()) {
            item.getRoadmap().complete();
            log.info("로드맵 전체 완료: roadmapId={}", item.getRoadmap().getId());
        }

        return RoadmapResponse.RoadmapItemResponse.from(item);
    }

    // 강좌 완료 취소
    public RoadmapResponse.RoadmapItemResponse uncompleteItem(String itemId, String userId) {
        RoadmapItem item = getItemAndValidate(itemId, userId);
        item.uncomplete();
        return RoadmapResponse.RoadmapItemResponse.from(item);
    }

    // ─── 내부 헬퍼 ───────────────────────────────────────────

    private Roadmap getAndValidate(String roadmapId, String userId) {
        Roadmap roadmap = roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new RuntimeException("로드맵을 찾을 수 없습니다"));
        if (!roadmap.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }
        return roadmap;
    }

    private RoadmapItem getItemAndValidate(String itemId, String userId) {
        RoadmapItem item = roadmapItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("강좌 항목을 찾을 수 없습니다"));
        if (!item.getRoadmap().getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }
        return item;
    }

    // 엔티티 → 응답 DTO 변환
    private RoadmapResponse buildRoadmapResponse(Roadmap roadmap) {
        List<RoadmapItem> items = roadmap.getItems() != null ? roadmap.getItems() : List.of();
        List<RoadmapWeek> weeks = roadmap.getWeeks() != null ? roadmap.getWeeks() : List.of();

        Map<Integer, List<RoadmapItem>> itemsByWeek =
                items.stream().collect(Collectors.groupingBy(RoadmapItem::getWeekNumber));

        List<RoadmapResponse.WeekResponse> weekResponses = weeks.stream()
                .map(week -> {
                    List<RoadmapItem> wi = itemsByWeek.getOrDefault(week.getWeekNumber(), List.of());
                    long done = wi.stream().filter(RoadmapItem::getIsCompleted).count();
                    return RoadmapResponse.WeekResponse.builder()
                            .weekNumber(week.getWeekNumber())
                            .title(week.getTitle())
                            .description(week.getDescription())
                            .totalHours(week.getTotalHours())
                            .completedCount((int) done)
                            .totalCount(wi.size())
                            .weekProgressPercent(wi.isEmpty() ? 0 : (double) done / wi.size() * 100)
                            .items(wi.stream()
                                    .map(RoadmapResponse.RoadmapItemResponse::from)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        long done = items.stream().filter(RoadmapItem::getIsCompleted).count();
        double progress = items.isEmpty() ? 0 : (double) done / items.size() * 100;

        return RoadmapResponse.builder()
                .id(roadmap.getId())
                .goal(roadmap.getGoal())
                .prompt(roadmap.getPrompt())
                .totalWeeks(roadmap.getTotalWeeks())
                .weeklyHours(roadmap.getWeeklyHours())
                .totalCourses(roadmap.getTotalCourses())
                .isActive(roadmap.getIsActive())
                .isCompleted(roadmap.getIsCompleted())
                .currentWeek(roadmap.currentWeek())
                .progressPercent(progress)
                .createdAt(roadmap.getCreatedAt())
                .activatedAt(roadmap.getActivatedAt())
                .completedAt(roadmap.getCompletedAt())
                .weeks(weekResponses)
                .build();
    }
}
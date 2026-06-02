package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.roadmap.response.RoadmapResponse;
import com.example.kuriq.entity.roadmap.*;
import com.example.kuriq.entity.user.User;
import com.example.kuriq.repository.notification.NotificationSettingRepository;
import com.example.kuriq.repository.roadmap.*;
import com.example.kuriq.repository.user.UserRepository;
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
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final BadgeService badgeService;
    private static final String DASHBOARD_URL = "https://kuriq.com/dashboard";

    // 로드맵 생성
    public RoadmapResponse generateRoadmap(String prompt, String userId) {

        // AI 호출
        AiClient.RoadmapGenerateAiResponse ai = aiClient.generateRoadmap(
                AiClient.RoadmapGenerateAiRequest.builder()
                        .prompt(prompt)
                        .userId(userId)
                        .build());

        // AI 응답 courseId → 강좌 조회
        // AI 가 반환하는 courseId 는 "K-MOOC_19921" 형식 (platform_platformCourseId)
        List<String> courseIds = ai.getWeeks().stream()
                .flatMap(w -> w.getCourses().stream())
                .map(AiClient.RoadmapGenerateAiResponse.CourseItemDto::getCourseId)
                .distinct()
                .collect(Collectors.toList());

        log.info("[RoadmapService] AI 가 반환한 courseId 목록: {}", courseIds);

        // courseId 를 platform + platformCourseId 로 분리하여 강좌 조회
        Map<String, Course> courseMap = new java.util.HashMap<>();
        List<String> missingIds = new java.util.ArrayList<>();
        
        for (String courseId : courseIds) {
            String[] parts = courseId.split("_", 2);
            if (parts.length != 2) {
                log.warn("[RoadmapService] 잘못된 courseId 형식: {}", courseId);
                continue;
            }
            String platformStr = parts[0];
            String platformCourseId = parts[1];
            
            try {
                Platform platform = Platform.valueOf(platformStr.replace("-", "_"));
                courseRepository.findByPlatformAndPlatformCourseId(platform, platformCourseId)
                        .ifPresent(course -> courseMap.put(courseId, course));
            } catch (IllegalArgumentException e) {
                log.warn("[RoadmapService] 잘못된 platform: {}", platformStr);
            }
            
            // 없는 강좌는 목록에 추가
            if (!courseMap.containsKey(courseId)) {
                missingIds.add(courseId);
            }
        }

        log.info("[RoadmapService] DB 에서 찾은 강좌 수: {}/{}", courseMap.size(), courseIds.size());
        
        // 없는 강좌는 chromaDB 에서 메타데이터 조회하여 DB 에 저장
        if (!missingIds.isEmpty()) {
            log.warn("[RoadmapService] DB 에 없는 courseId: {}", missingIds);
            log.info("[RoadmapService] chromaDB 에서 메타데이터 조회 시작...");
            
            AiClient.CourseMetadataResponse metadataResponse = aiClient.getCourseMetadata(missingIds);
            if (metadataResponse != null && metadataResponse.getCourses() != null) {
                for (AiClient.CourseMetadataResponse.CourseMetadataDto dto : metadataResponse.getCourses()) {
                    // chromaDB 데이터로 Course 객체 생성 (ID 는 DB 에서 자동 생성)
                    Course chromaCourse = Course.builder()
                            .title(dto.getTitle())
                            .platform(parsePlatform(dto.getPlatform()))
                            .platformCourseId(extractPlatformCourseId(dto.getCourseId()))
                            .institution(dto.getInstitution())
                            .category(dto.getCategory())
                            .difficulty(dto.getDifficulty())
                            .durationWeeks(dto.getDurationWeeks() != null ? dto.getDurationWeeks() : 0)
                            .estimatedHours(dto.getEstimatedHours() != null ? java.math.BigDecimal.valueOf(dto.getEstimatedHours()) : java.math.BigDecimal.ZERO)
                            .hasCertificate(dto.getHasCertificate() != null ? dto.getHasCertificate() : false)
                            .url(dto.getUrl() != null && !dto.getUrl().isEmpty() ? dto.getUrl() : "#")
                            .isActive(true)
                            .build();
                    
                    // DB 에 저장 (중복이면 기존 것 사용)
                    try {
                        courseRepository.save(chromaCourse);
                        // 저장 후 생성된 ID 로 courseMap 업데이트
                        courseMap.put(dto.getCourseId(), chromaCourse);
                        log.info("[RoadmapService] Course 저장 완료: {} (UUID: {})", dto.getCourseId(), chromaCourse.getId());
                    } catch (Exception e) {
                        log.warn("[RoadmapService] Course 저장 실패 (중복), 기존 강좌 조회: {}", dto.getCourseId());
                        // 이미 있으면 다시 조회
                        courseRepository.findByPlatformAndPlatformCourseId(chromaCourse.getPlatform(), chromaCourse.getPlatformCourseId())
                                .ifPresentOrElse(
                                        existing -> {
                                            courseMap.put(dto.getCourseId(), existing);
                                            log.info("[RoadmapService] 기존 강좌 사용: {} (UUID: {})", dto.getCourseId(), existing.getId());
                                        },
                                        () -> log.error("[RoadmapService] 강좌를 찾을 수 없음: {}", dto.getCourseId())
                                );
                    }
                }
                log.info("[RoadmapService] chromaDB 에서 {}개 강좌 메타데이터 조회 및 저장 완료", metadataResponse.getCourses().size());
            }
        }
        
        // 최종적으로 모든 courseId 가 courseMap 에 있는지 확인
        long missingCount = courseIds.stream().filter(id -> !courseMap.containsKey(id)).count();
        if (missingCount > 0) {
            log.error("[RoadmapService] {}개의 강좌를 찾지 못함. 로드맵 생성을 계속할 수 없습니다.", missingCount);
            throw new IllegalStateException("강좌 정보를 찾을 수 없습니다.");
        }

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
            roadmap.getWeeks().add(week);  // ★ 추가 — 컬렉션에 직접 add

            for (AiClient.RoadmapGenerateAiResponse.CourseItemDto itemDto : weekDto.getCourses()) {
                Course course = courseMap.get(itemDto.getCourseId());
                if (course == null) {
                    log.warn("AI courseId 없음: {}", itemDto.getCourseId());
                    continue;
                }
                RoadmapItem item = RoadmapItem.create(roadmap, course, weekDto.getWeekNumber(), itemDto.getOrderInWeek());
                roadmapItemRepository.save(item);
                roadmap.getItems().add(item);  // ★ 추가 — 컬렉션에 직접 add
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
        boolean roadmapJustCompleted = false;

        if (item.getRoadmap().allItemsCompleted()) {
            item.getRoadmap().complete();
            roadmapJustCompleted = true;
            log.info("로드맵 전체 완료: roadmapId={}", item.getRoadmap().getId());

            // 완료 축하 알림 (로드맵 전체 완료 시에만 발송)
            notificationSettingRepository.findCompletionAlertTarget(userId)
                    .ifPresent(ns -> {
                        User user = userRepository.findById(userId).orElse(null);
                        if (user != null && user.getEmail() != null) {
                            emailService.sendCompletionEmail(
                                    user.getEmail(), userId, user.getName(),
                                    item.getRoadmap().getGoal(), DASHBOARD_URL);
                        }
                    });
        }

        // 뱃지 체크 — @Async 이므로 현재 트랜잭션과 분리되어 실행됨
        final boolean finalRoadmapJustCompleted = roadmapJustCompleted;
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        badgeService.checkAndAwardOnCourseComplete(userId);
                        if (finalRoadmapJustCompleted) {
                            badgeService.checkAndAwardOnRoadmapComplete(userId);
                        }
                    }
                });

        return RoadmapResponse.RoadmapItemResponse.from(item);
    }

    // 강좌 완료 취소
    public RoadmapResponse.RoadmapItemResponse uncompleteItem(String itemId, String userId) {
        RoadmapItem item = getItemAndValidate(itemId, userId);
        item.uncomplete();
        return RoadmapResponse.RoadmapItemResponse.from(item);
    }

    /** 데이터 조회 후 사용자 권한까지 검증하는 공통 메서드 */
    // MultipleBagFetchException 에러 고치기 -> 한 번에 하나의 Bag만 fetch하기
    private Roadmap getAndValidate(String roadmapId, String userId) {
        // 1차 조회: weeks 로드
        Roadmap roadmap = roadmapRepository.findByIdWithWeeks(roadmapId)
                .orElseThrow(() -> new RuntimeException("로드맵을 찾을 수 없습니다"));

        if (!roadmap.getUserId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다");
        }

        // 2차 조회: items + course 로드 (영속성 컨텍스트가 같으므로 자동 병합됨)
        roadmapRepository.findByIdWithItems(roadmapId);

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

    private Platform parsePlatform(String platformStr) {
        if (platformStr == null || platformStr.isBlank()) {
            return Platform.K_MOOC;  // 기본값
        }
        try {
            return Platform.valueOf(platformStr.replace("-", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[RoadmapService] 잘못된 platform: {}, 기본값 사용", platformStr);
            return Platform.K_MOOC;
        }
    }

    private String extractPlatformCourseId(String courseId) {
        if (courseId == null || !courseId.contains("_")) {
            return courseId;
        }
        // "K-MOOC_19921" → "19921"
        return courseId.substring(courseId.indexOf("_") + 1);
    }

    // 엔티티 -> 응답 DTO 변환
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
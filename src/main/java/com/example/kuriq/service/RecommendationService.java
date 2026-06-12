package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.course.response.NextCourseResponse;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.LearningHistory;
import com.example.kuriq.entity.roadmap.Platform;
import com.example.kuriq.entity.roadmap.Roadmap;
import com.example.kuriq.entity.roadmap.RoadmapItem;
import com.example.kuriq.repository.roadmap.CourseRepository;
import com.example.kuriq.repository.roadmap.LearningHistoryRepository;
import com.example.kuriq.repository.roadmap.RoadmapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final LearningHistoryRepository learningHistoryRepository;
    private final CourseRepository courseRepository;
    private final RoadmapRepository roadmapRepository;
    private final AiClient aiClient;

    public List<NextCourseResponse> getRecommendation(String userId, String roadmapId) {

        Optional<RecommendationSeed> seedOpt = resolveSeed(userId, roadmapId);
        if (seedOpt.isEmpty()) {
            return List.of();
        }

        RecommendationSeed seed = seedOpt.get();
        Course baseCourse = seed.course();
        String category = baseCourse.getCategory();

        if (category == null || category.isBlank()) {
            return List.of();
        }

        // 3. AI 서버에 벡터 유사도 기반 추천 요청
        String aiCourseId = baseCourse.getId();

        AiClient.RecommendationAiResponse aiResponse;
        try {
            aiResponse = aiClient.getRecommendations(
                    AiClient.RecommendationAiRequest.builder()
                            .courseId(aiCourseId)
                            .courseTitle(baseCourse.getTitle())
                            .category(category)
                            .top_k(5)
                            .build()
            );
        } catch (Exception e) {
            log.error("[추천] AI 서버 호출 실패: {}", e.getMessage());
            // AI 호출 실패 시 MySQL 폴백
            return fallbackFromMySQL(category, baseCourse.getId());
        }

        if (aiResponse == null || aiResponse.getCourses() == null || aiResponse.getCourses().isEmpty()) {
            // AI 응답 없을 시 MySQL 폴백
            return fallbackFromMySQL(category, baseCourse.getId());
        }

        // 4. AI 서버 응답 강좌 최대 3개를 MySQL에서 조회
        List<NextCourseResponse> result = new ArrayList<>();
        Set<String> addedCourseIds = new HashSet<>();  // 중복 방지용

        for (AiClient.RecommendationAiResponse.RecommendationCourseDto recommended
                : aiResponse.getCourses().subList(0, Math.min(3, aiResponse.getCourses().size()))) {
            String courseIdRaw = recommended.getCourse_id();
            String message = buildMessage(seed, recommended.getTitle());

            Optional<Course> directCourseOpt = courseRepository.findById(courseIdRaw);
            if (directCourseOpt.isPresent()) {
                Course course = directCourseOpt.get();
                if (addedCourseIds.contains(course.getId())) continue;
                addedCourseIds.add(course.getId());
                result.add(NextCourseResponse.from(course, message));
                continue;
            }

            // course_id 형식: "LLL_PORTAL_2390297" → platform + platformCourseId 분리
            int lastIdx = courseIdRaw.lastIndexOf("_");
            if (lastIdx < 0) continue;

            String platformStr = courseIdRaw.substring(0, lastIdx);   // LLL_PORTAL
            String platformCourseId = courseIdRaw.substring(lastIdx + 1); // 2390297

            Optional<Course> courseOpt = Optional.empty();
            try {
                Platform platform = Platform.valueOf(platformStr);
                courseOpt = courseRepository.findByPlatformAndPlatformCourseId(platform, platformCourseId);
            } catch (IllegalArgumentException e) {
                log.warn("[추천] 알 수 없는 플랫폼: {}", platformStr);
            }

            if (courseOpt.isPresent()) {
                Course course = courseOpt.get();
                // 중복 제거
                if (addedCourseIds.contains(course.getId())) continue;
                addedCourseIds.add(course.getId());
                result.add(NextCourseResponse.from(course, message));
            } else {
                // url 없으면 표시 의미 없으므로 제외
                String url = recommended.getUrl();
                if (url == null || url.isBlank()) {
                    log.warn("[추천] courses 테이블에 없고 url도 없음: {}", courseIdRaw);
                    continue;
                }
                // courses 테이블에 없으면 AI 응답 데이터로 직접 구성
                if (addedCourseIds.contains(courseIdRaw)) continue;
                addedCourseIds.add(courseIdRaw);
                result.add(NextCourseResponse.builder()
                        .courseId(courseIdRaw)
                        .title(recommended.getTitle())
                        .institution(recommended.getInstitution())
                        .platform(null)
                        .category(recommended.getCategory())
                        .difficulty(null)
                        .estimatedHours(null)
                        .url(url)
                        .hasCertificate(null)
                        .message(message)
                        .build());
            }
        }

        // AI 응답이 있었지만 매칭되는 강좌가 없는 경우 MySQL 폴백
        if (result.isEmpty()) {
            return fallbackFromMySQL(category, baseCourse.getId());
        }

        return result;
    }

    // AI 추천 실패 시 MySQL에서 같은 카테고리 강좌 3개 직접 조회
    private List<NextCourseResponse> fallbackFromMySQL(String category, String excludeCourseId) {
        log.info("[추천] MySQL 폴백 실행: category={}", category);
        List<Course> courses = courseRepository
                .findTop3ByCategoryAndIsActiveTrueAndIdNotOrderByIdAsc(category, excludeCourseId);
        return courses.stream()
                .map(c -> NextCourseResponse.from(c,
                        String.format("%s 분야의 추천 강좌예요!", category)))
                .toList();
    }

    private Optional<RecommendationSeed> resolveSeed(String userId, String roadmapId) {
        if (roadmapId != null && !roadmapId.isBlank()) {
            Optional<RecommendationSeed> roadmapSeed = findRoadmapSeed(userId, roadmapId);
            if (roadmapSeed.isPresent()) {
                return roadmapSeed;
            }
        }
        return findLatestCompletedSeed(userId);
    }

    private Optional<RecommendationSeed> findRoadmapSeed(String userId, String roadmapId) {
        Optional<Roadmap> roadmapOpt = roadmapRepository.findByIdWithItems(roadmapId);
        if (roadmapOpt.isEmpty()) {
            return Optional.empty();
        }

        Roadmap roadmap = roadmapOpt.get();
        if (!roadmap.getUserId().equals(userId)) {
            log.warn("[추천] 다른 사용자의 로드맵 접근 시도: roadmapId={}", roadmapId);
            return Optional.empty();
        }

        List<RoadmapItem> items = roadmap.getItems();
        if (items == null || items.isEmpty()) {
            return Optional.empty();
        }

        Optional<RoadmapItem> latestCompleted = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsCompleted()))
                .filter(item -> item.getCompletedAt() != null)
                .filter(item -> item.getCourse() != null)
                .max(Comparator.comparing(RoadmapItem::getCompletedAt));

        if (latestCompleted.isPresent()) {
            return Optional.of(new RecommendationSeed(latestCompleted.get().getCourse(), true));
        }

        Optional<RoadmapItem> currentItem = items.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsCompleted()))
                .filter(item -> item.getCourse() != null)
                .min(Comparator.comparing(RoadmapItem::getWeekNumber)
                        .thenComparing(RoadmapItem::getOrderInWeek));

        if (currentItem.isPresent()) {
            return Optional.of(new RecommendationSeed(currentItem.get().getCourse(), false));
        }

        return items.stream()
                .filter(item -> item.getCourse() != null)
                .max(Comparator.comparing(RoadmapItem::getWeekNumber)
                        .thenComparing(RoadmapItem::getOrderInWeek))
                .map(item -> new RecommendationSeed(item.getCourse(), true));
    }

    private Optional<RecommendationSeed> findLatestCompletedSeed(String userId) {
        List<LearningHistory> histories = learningHistoryRepository.findByUserIdOrderByCompletedAtDesc(
                userId,
                PageRequest.of(0, 1)
        );

        if (histories.isEmpty()) {
            return Optional.empty();
        }

        return courseRepository.findById(histories.get(0).getCourseId())
                .map(course -> new RecommendationSeed(course, true));
    }

    // 큐리 추천 메시지 생성
    private String buildMessage(RecommendationSeed seed, String nextTitle) {
        String lastTitle = seed.course().getTitle();

        if (!seed.completedBased()) {
            return String.format("현재 보고 있는 %s 흐름과 이어서 %s 과정도 추천해요!",
                    lastTitle, nextTitle);
        }

        // 마지막 글자 받침 여부에 따라 을/를 선택
        char lastChar = lastTitle.charAt(lastTitle.length() - 1);
        boolean hasBatchim = (lastChar - 0xAC00) % 28 != 0;
        String eul = hasBatchim ? "을" : "를";

        return String.format("%s%s 잘 마무리했어요! 다음으로 %s 과정은 어떨까요?",
                lastTitle, eul, nextTitle);
    }

    private record RecommendationSeed(Course course, boolean completedBased) {
    }
}

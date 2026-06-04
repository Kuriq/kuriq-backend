package com.example.kuriq.service;

import com.example.kuriq.client.AiClient;
import com.example.kuriq.dto.course.response.NextCourseResponse;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.LearningHistory;
import com.example.kuriq.entity.roadmap.Platform;
import com.example.kuriq.repository.roadmap.CourseRepository;
import com.example.kuriq.repository.roadmap.LearningHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final LearningHistoryRepository learningHistoryRepository;
    private final CourseRepository courseRepository;
    private final AiClient aiClient;

    public List<NextCourseResponse> getRecommendation(String userId) {

        // 1. 가장 최근 이수한 강좌 조회
        List<LearningHistory> histories =
                learningHistoryRepository.findByUserIdOrderByCompletedAtDesc(userId,
                        PageRequest.of(0, 1));

        if (histories.isEmpty()) {
            return List.of();
        }

        String lastCourseId = histories.get(0).getCourseId();

        // 2. 해당 강좌의 카테고리 조회
        Optional<Course> lastCourseOpt = courseRepository.findById(lastCourseId);
        if (lastCourseOpt.isEmpty()) {
            return List.of();
        }

        Course lastCourse = lastCourseOpt.get();
        String category = lastCourse.getCategory();

        if (category == null) {
            return List.of();
        }

        // 3. AI 서버에 벡터 유사도 기반 추천 요청
        String aiCourseId = lastCourse.getPlatform().name() + "_" + lastCourse.getPlatformCourseId();

        AiClient.RecommendationAiResponse aiResponse;
        try {
            aiResponse = aiClient.getRecommendations(
                    AiClient.RecommendationAiRequest.builder()
                            .courseId(aiCourseId)
                            .courseTitle(lastCourse.getTitle())
                            .category(category)
                            .top_k(5)
                            .build()
            );
        } catch (Exception e) {
            log.error("[추천] AI 서버 호출 실패: {}", e.getMessage());
            return List.of();
        }

        if (aiResponse == null || aiResponse.getCourses() == null || aiResponse.getCourses().isEmpty()) {
            return List.of();
        }

        // 4. AI 서버 응답 강좌 최대 3개를 MySQL에서 조회
        List<NextCourseResponse> result = new ArrayList<>();

        for (AiClient.RecommendationAiResponse.RecommendationCourseDto recommended
                : aiResponse.getCourses().subList(0, Math.min(3, aiResponse.getCourses().size()))) {

            // course_id 형식: "LLL_PORTAL_2390297" → platform + platformCourseId 분리
            String courseIdRaw = recommended.getCourse_id();
            int lastIdx = courseIdRaw.lastIndexOf("_");
            if (lastIdx < 0) continue;

            String platformStr = courseIdRaw.substring(0, lastIdx);   // LLL_PORTAL
            String platformCourseId = courseIdRaw.substring(lastIdx + 1); // 2390297
            String message = buildMessage(lastCourse.getTitle(), recommended.getTitle());

            Optional<Course> courseOpt = Optional.empty();
            try {
                Platform platform = Platform.valueOf(platformStr);
                courseOpt = courseRepository.findByPlatformAndPlatformCourseId(platform, platformCourseId);
            } catch (IllegalArgumentException e) {
                log.warn("[추천] 알 수 없는 플랫폼: {}", platformStr);
            }

            if (courseOpt.isPresent()) {
                result.add(NextCourseResponse.from(courseOpt.get(), message));
            } else {
                // courses 테이블에 없으면 AI 응답 데이터로 직접 구성
                result.add(NextCourseResponse.builder()
                        .courseId(recommended.getCourse_id())
                        .title(recommended.getTitle())
                        .institution(recommended.getInstitution())
                        .platform(null)
                        .category(recommended.getCategory())
                        .difficulty(null)
                        .estimatedHours(null)
                        .url(null)
                        .hasCertificate(null)
                        .message(message)
                        .build());
            }
        }
        return result;
    }

    // 큐리 추천 메시지 생성
    private String buildMessage(String lastTitle, String nextTitle) {
        // 마지막 글자 받침 여부에 따라 을/를 선택
        char lastChar = lastTitle.charAt(lastTitle.length() - 1);
        boolean hasBatchim = (lastChar - 0xAC00) % 28 != 0;
        String eul = hasBatchim ? "을" : "를";

        return String.format("%s%s 잘 마무리했어요! 다음으로 %s 과정은 어떨까요?",
                lastTitle, eul, nextTitle);
    }
}
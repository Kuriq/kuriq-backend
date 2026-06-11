package com.example.kuriq.service;

import com.example.kuriq.dto.course.request.CourseSearchRequest;
import com.example.kuriq.dto.course.response.CourseResponse;
import com.example.kuriq.dto.course.response.CourseSearchResponse;
import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.Platform;
import com.example.kuriq.repository.roadmap.CourseRepository;
import com.example.kuriq.util.CourseCategoryResolver;
import com.example.kuriq.util.CoursePlatformLabelResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;
    private final WebClient aiWebClient;

    @Value("${ai.service.base-url}")
    private String aiServiceUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public CourseSearchResponse search(CourseSearchRequest request) {
        // chromaDB 검색 사용 (AI 서버 프록시)
        return searchFromChromaDb(request);
    }

    private CourseSearchResponse searchFromChromaDb(CourseSearchRequest request) {
        try {
            log.info("[CourseService] chromaDB 검색 시작: keyword={}, platform={}, category={}", 
                request.getKeyword(), request.getPlatform(), request.getCategory());
            
            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("keyword", request.getKeyword())
                    .put("platform", request.getPlatform())
                    .put("category", request.getCategory())
                    .put("difficulty", request.getDifficulty())
                    .put("page", request.getPage())
                    .put("size", request.getSize());

            log.info("[CourseService] AI 서버 요청: {}", aiServiceUrl + "/internal/ai/courses/search");

            JsonNode response = aiWebClient.post()
                    .uri("/internal/ai/courses/search")
                    .header("X-Internal-Key", internalApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null) {
                throw new RuntimeException("AI 서버 응답이 없습니다.");
            }

            if (response.get("totalElements").asLong() == 0) {
                throw new RuntimeException("ChromaDB 결과 없음, MySQL fallback");
            }

            log.info("[CourseService] AI 서버 응답: totalElements={}", response.get("totalElements").asLong());

            // 응답 파싱
            List<CourseResponse> content = objectMapper.convertValue(
                    response.get("content"),
                    new com.fasterxml.jackson.core.type.TypeReference<List<CourseResponse>>() {}
            );

            return CourseSearchResponse.builder()
                    .content(content)
                    .totalElements(response.get("totalElements").asLong())
                    .totalPages(response.get("totalPages").asInt())
                    .currentPage(response.get("currentPage").asInt())
                    .size(response.get("size").asInt())
                    .hasNext(response.get("hasNext").asBoolean())
                    .build();

        } catch (Exception e) {
            log.error("chromaDB 검색 실패: {}", e.getMessage(), e);
            // fallback: MySQL 검색
            log.warn("[CourseService] MySQL 로 fallback 합니다.");
            return searchFromMysql(request);
        }
    }

    private CourseSearchResponse searchFromMysql(CourseSearchRequest request) {
        Specification<Course> spec = Specification.where((root, query, cb) -> cb.isTrue(root.get("isActive")));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("institution")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)
            ));
        }
        if (request.getPlatform() != null && !request.getPlatform().isBlank()) {
            Platform platform = CoursePlatformLabelResolver.parseRequestedPlatform(request.getPlatform());
            if (platform != null) {
                spec = spec.and(platformSpec(platform));
            }
        }
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) spec = spec.and(eq("difficulty", request.getDifficulty()));
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Set<String> aliases = CourseCategoryResolver.categoryAliases(request.getCategory());
            spec = spec.and((root, query, cb) -> cb.or(
                    aliases.stream().map(alias -> cb.like(root.get("category"), "%" + alias + "%"))
                            .toArray(jakarta.persistence.criteria.Predicate[]::new)
            ));
        }
        if (request.getHasCertificate() != null) spec = spec.and(eq("hasCertificate", request.getHasCertificate()));
        if (request.getDurationRange() != null && !request.getDurationRange().isBlank()) spec = spec.and(durationSpec(request.getDurationRange()));

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sortFor(request.getSort()));
        Page<Course> page = courseRepository.findAll(spec, pageable);
        return CourseSearchResponse.from(page);
    }

    private Specification<Course> eq(String field, Object value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private Specification<Course> platformSpec(Platform platform) {
        return switch (platform) {
            case K_MOOC -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("platform"), Platform.K_MOOC),
                    cb.like(root.get("institution"), "K-MOOC%")
            );
            case KOCW -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("platform"), Platform.KOCW),
                    cb.like(root.get("institution"), "KOCW%")
            );
            case SEOUL_LLL -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("platform"), Platform.SEOUL_LLL),
                    cb.like(root.get("institution"), "서울시평생학습포털%"),
                    cb.like(root.get("institution"), "서울시 평생학습포털%")
            );
            default -> eq("platform", platform);
        };
    }

    private Specification<Course> durationSpec(String range) {
        String value = range.trim();
        try {
            if (value.matches("^\\d+-\\d+$")) {
                String[] parts = value.split("-");
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return (root, query, cb) -> cb.between(root.get("durationWeeks"), min, max);
            }
            if (value.matches("^\\d+\\+$")) {
                int min = Integer.parseInt(value.substring(0, value.length() - 1));
                return (root, query, cb) -> cb.ge(root.get("durationWeeks"), min);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("durationRange 형식이 올바르지 않습니다.");
        }
        throw new IllegalArgumentException("durationRange 형식이 올바르지 않습니다.");
    }

    private Sort sortFor(String sort) {
        return switch (sort == null ? "latest" : sort) {
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            case "duration" -> Sort.by(Sort.Direction.ASC, "durationWeeks");
            case "hours" -> Sort.by(Sort.Direction.ASC, "estimatedHours");
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}

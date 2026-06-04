package com.example.kuriq.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    private static final String SEARCH_BY_CATEGORY_PATH = "/v2/local/search/category.json";
    private static final String SEARCH_BY_KEYWORD_PATH = "/v2/local/search/keyword.json";
    private static final String CAFE_CATEGORY_GROUP_CODE = "CE7";
    private static final String STUDY_CAFE_KEYWORD = "스터디카페";
    private static final int API_MAX_SIZE = 15;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;

    @Value("${kakao.local.base-url:https://dapi.kakao.com}")
    private String kakaoLocalBaseUrl;

    @Value("${kakao.local.rest-api-key:}")
    private String kakaoRestApiKey;

    @Value("${oauth.kakao.client-id:}")
    private String kakaoOauthClientId;

    public List<KakaoPlace> searchNearbyPrivateSpaces(double lat, double lng, int radius, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        String effectiveApiKey = resolveKakaoApiKey();
        if (!StringUtils.hasText(effectiveApiKey)) {
            log.warn("Kakao Local REST API key is not configured. Set KAKAO_LOCAL_REST_API_KEY or oauth.kakao.client-id. Returning only public study spaces.");
            return List.of();
        }

        Map<String, KakaoPlace> deduplicated = new LinkedHashMap<>();

        safeFetchByCategory(effectiveApiKey, lng, lat, radius, Math.min(limit, API_MAX_SIZE))
                .forEach(place -> deduplicated.putIfAbsent(place.id(), place));

        safeFetchByKeyword(effectiveApiKey, STUDY_CAFE_KEYWORD, lng, lat, radius, Math.min(limit, API_MAX_SIZE))
                .forEach(place -> deduplicated.putIfAbsent(place.id(), place));

        return deduplicated.values().stream()
                .sorted(Comparator.comparingInt(KakaoPlace::distanceMeters))
                .limit(limit)
                .toList();
    }

    private List<KakaoPlace> fetchByCategory(String apiKey, double lng, double lat, int radius, int size) {
        return get(apiKey)
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_BY_CATEGORY_PATH)
                        .queryParam("category_group_code", CAFE_CATEGORY_GROUP_CODE)
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("radius", radius)
                        .queryParam("size", size)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(KakaoLocalSearchResponse.class)
                .blockOptional(REQUEST_TIMEOUT)
                .map(KakaoLocalSearchResponse::documents)
                .orElseGet(List::of);
    }

    private List<KakaoPlace> fetchByKeyword(String apiKey, String keyword, double lng, double lat, int radius, int size) {
        return get(apiKey)
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_BY_KEYWORD_PATH)
                        .queryParam("query", keyword)
                        .queryParam("x", lng)
                        .queryParam("y", lat)
                        .queryParam("radius", radius)
                        .queryParam("size", size)
                        .queryParam("sort", "distance")
                        .build())
                .retrieve()
                .bodyToMono(KakaoLocalSearchResponse.class)
                .blockOptional(REQUEST_TIMEOUT)
                .map(KakaoLocalSearchResponse::documents)
                .orElseGet(List::of);
    }

    private List<KakaoPlace> safeFetchByCategory(String apiKey, double lng, double lat, int radius, int size) {
        try {
            return fetchByCategory(apiKey, lng, lat, radius, size);
        } catch (Exception e) {
            log.warn("Failed to fetch Kakao category places: {}", e.getMessage());
            return List.of();
        }
    }

    private List<KakaoPlace> safeFetchByKeyword(String apiKey, String keyword, double lng, double lat, int radius, int size) {
        try {
            return fetchByKeyword(apiKey, keyword, lng, lat, radius, size);
        } catch (Exception e) {
            log.warn("Failed to fetch Kakao keyword places: {}", e.getMessage());
            return List.of();
        }
    }

    private WebClient.RequestHeadersUriSpec<?> get(String apiKey) {
        return webClientBuilder
                .baseUrl(kakaoLocalBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .build()
                .get();
    }

    private String resolveKakaoApiKey() {
        if (isUsableKey(kakaoRestApiKey)) {
            return kakaoRestApiKey;
        }

        if (isUsableKey(kakaoOauthClientId)) {
            return kakaoOauthClientId;
        }

        return "";
    }

    private boolean isUsableKey(String value) {
        return StringUtils.hasText(value) && !value.startsWith("your-");
    }

    private record KakaoLocalSearchResponse(List<KakaoPlace> documents) {
    }

    public record KakaoPlace(
            String id,
            String place_name,
            String category_name,
            String phone,
            String address_name,
            String road_address_name,
            String x,
            String y,
            String place_url,
            String distance
    ) {
        public int distanceMeters() {
            try {
                return distance == null ? Integer.MAX_VALUE : Integer.parseInt(distance);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }

        public String resolvedAddress() {
            return StringUtils.hasText(road_address_name) ? road_address_name : address_name;
        }
    }
}

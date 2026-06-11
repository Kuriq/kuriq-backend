package com.example.kuriq.util;

import com.example.kuriq.entity.roadmap.Platform;

public final class CoursePlatformLabelResolver {

    private CoursePlatformLabelResolver() {
    }

    public static String resolvePlatform(Platform platform, String institution) {
        Platform inferred = inferPlatform(platform, institution);
        return inferred != null ? inferred.name() : (platform != null ? platform.name() : null);
    }

    public static String normalizeInstitution(String institution, Platform platform) {
        String value = institution == null ? null : institution.trim();
        if (value == null || value.isBlank()) {
            Platform inferred = inferPlatform(platform, institution);
            return inferred != null ? displayLabel(inferred) : value;
        }

        Platform exactAlias = exactAlias(value);
        return exactAlias != null ? displayLabel(exactAlias) : value;
    }

    public static Platform inferPlatform(Platform platform, String institution) {
        Platform alias = aliasFromInstitution(institution);
        return alias != null ? alias : platform;
    }

    public static Platform parseRequestedPlatform(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        Platform alias = exactAlias(value);
        if (alias != null) {
            return alias;
        }

        try {
            return Platform.valueOf(value.replace("-", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Platform aliasFromInstitution(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        Platform exact = exactAlias(value);
        if (exact != null) {
            return exact;
        }

        if (value.startsWith("K-MOOC")) {
            return Platform.K_MOOC;
        }
        if (value.startsWith("KOCW")) {
            return Platform.KOCW;
        }
        if (value.startsWith("서울시평생학습포털") || value.startsWith("서울시 평생학습포털")) {
            return Platform.SEOUL_LLL;
        }

        return null;
    }

    private static Platform exactAlias(String raw) {
        return switch (raw) {
            case "K_MOOC", "K-MOOC", "KMOOC" -> Platform.K_MOOC;
            case "KOCW" -> Platform.KOCW;
            case "LLL_PORTAL", "ALLGO", "온국민평생배움터" -> Platform.LLL_PORTAL;
            case "EVERLEARNING", "에버러닝", "전국평생학습" -> Platform.EVERLEARNING;
            case "SEOUL_LLL", "서울시평생학습포털", "서울시 평생학습포털" -> Platform.SEOUL_LLL;
            default -> null;
        };
    }

    private static String displayLabel(Platform platform) {
        return switch (platform) {
            case K_MOOC -> "K-MOOC";
            case KOCW -> "KOCW";
            case LLL_PORTAL -> "온국민평생배움터";
            case EVERLEARNING -> "전국평생학습";
            case SEOUL_LLL -> "서울시평생학습포털";
            default -> platform.name();
        };
    }
}

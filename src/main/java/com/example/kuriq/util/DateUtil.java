package com.example.kuriq.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 유틸리티 클래스
 */
public class DateUtil {

    private static final DateTimeFormatter KOREAN_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy 년 MM 월 dd 일");
    
    private static final DateTimeFormatter KOREAN_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime.atZone(KOREA_ZONE).format(KOREAN_DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime.atZone(KOREA_ZONE).format(KOREAN_TIME_FORMATTER);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return formatDate(dateTime) + " " + formatTime(dateTime);
    }

    public static LocalDateTime toKoreaTime(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(KOREA_ZONE).toLocalDateTime();
    }
}

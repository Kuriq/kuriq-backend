package com.example.kuriq.entity.badge;

public enum BadgeType {

    // 학습 첫 시작
    SEEDLING("새싹 학습자", "첫 강좌를 완료했어요!"),

    // 연속 학습 스트릭
    STREAK_3("3일 불꽃", "3일 연속 강좌를 완료했어요!"),
    STREAK_7("일주일 완주", "7일 연속 강좌를 완료했어요!"),
    STREAK_30("한 달의 기적", "30일 연속 강좌를 완료했어요!"),
    STREAK_100("멈출 수 없어", "100일 연속 강좌를 완료했어요!"),

    // 누적 강좌 완료
    COURSE_5("다섯 걸음", "강좌를 누적 5개 완료했어요!"),
    COURSE_10("열 걸음", "강좌를 누적 10개 완료했어요!"),
    COURSE_30("학습 고수", "강좌를 누적 30개 완료했어요!"),

    // 로드맵 전체 이수
    ROADMAP_1("길을 완주했다", "로드맵 1개를 전체 이수 완료했어요!"),
    ROADMAP_3("세 갈래 길", "로드맵 3개를 전체 이수 완료했어요!"),

    // 커뮤니티 활동
    FIRST_POST("드디어 한마디", "커뮤니티에 첫 게시글을 작성했어요!"),

    // 마스터 (위의 모든 뱃지 달성 시 자동 부여)
    QURI_MASTER("큐리 마스터", "모든 뱃지를 달성했어요!");

    // 뱃지 표시 이름
    private final String displayName;

    // 뱃지 달성 메시지
    private final String description;

    BadgeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription()  { return description; }
}

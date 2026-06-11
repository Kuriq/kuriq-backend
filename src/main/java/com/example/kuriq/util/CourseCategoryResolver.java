package com.example.kuriq.util;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

public final class CourseCategoryResolver {

    private static final Map<String, String[]> CATEGORY_ALIASES = Map.of(
            "IT·데이터", new String[]{"디지털", "컴퓨터", "소프트웨어", "데이터", "인공지능", "정보통신", "IT", "코딩", "프로그래밍", "AI", "머신러닝", "딥러닝", "빅데이터", "알고리즘", "웹", "앱", "모바일", "네트워크", "보안", "클라우드"},
            "경영·경제", new String[]{"경제/경영", "경영", "경제", "회계", "금융", "마케팅", "창업", "비즈니스", "관리", "리더십", "전략", "조직", "인사", "재무", "투자", "주식", "부동산"},
            "인문·교양", new String[]{"인문/교양", "인문", "철학", "역사", "문학", "심리", "교양", "고전", "사상", "종교", "신화", "인간", "사회사상", "윤리"},
            "외국어", new String[]{"영어", "중국어", "일본어", "외국어", "어학", "회화", "문법", "작문", "독해", "통역", "번역", "TESOL"},
            "자연과학·공학", new String[]{"수학", "물리", "화학", "생물", "공학", "기계", "전기", "건축", "과학", "환경", "에너지", "소재", "나노", "로봇", "항공", "우주", "지질", "천문"},
            "의료·보건", new String[]{"가족/건강/운동", "의학", "간호", "보건", "의료", "약학", "복지", "건강", "질병", "치료", "예방", "영양", "운동", "재활", "노인", "아동"},
            "예술·문화", new String[]{"예술", "음악", "미술", "디자인", "문화", "영상", "사진", "공연", "영화", "만화", "애니메이션", "패션", "공예", "무용", "연극"},
            "사회·법", new String[]{"사회", "법", "행정", "정치", "교육", "복지", "인권", "민주주의", "시민", "가족", "아동", "여성", "장애인", "다문화", "통일", "국제"},
            "취미·생활", new String[]{"취미", "생활", "요리", "여행", "스포츠", "원예", "반려동물", "레저", "게임", "독서", "글쓰기", "학습법", "자기계발"}
    );

    private CourseCategoryResolver() {
    }

    public static String normalizeCategory(String rawCategory) {
        String value = rawCategory == null ? "" : rawCategory.trim();
        if (value.isBlank()) {
            return "기타";
        }

        for (Map.Entry<String, String[]> entry : CATEGORY_ALIASES.entrySet()) {
            if (entry.getKey().equals(value)) {
                return entry.getKey();
            }

            for (String alias : entry.getValue()) {
                if (value.contains(alias)) {
                    return entry.getKey();
                }
            }
        }

        return value.contains("기타") ? "기타" : value;
    }

    public static Set<String> categoryAliases(String rawCategory) {
        String normalized = normalizeCategory(rawCategory);
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(normalized);

        String[] values = CATEGORY_ALIASES.get(normalized);
        if (values != null) {
            for (String value : values) {
                aliases.add(value);
            }
        }

        return aliases;
    }
}

package com.example.kuriq.entity.roadmap;

// 큐릭이 수집하는 공공 교육 플랫폼 종류
// ERD 의 platform_enum 과 동일
// Course, CourseClickLog 등 플랫폼을 다루는 곳에서 공통으로 사용하기 때문에 별도 파일로 분리했음
public enum Platform {
    K_MOOC,     // 한국형 온라인 공개강좌 (K-MOOC)
    KOCW,       // 대학 공개강의 서비스 (KOCW)
    LLL_PORTAL, // 온국민평생배움터 (all.go.kr)
    EVERLEARNING, // 에버러닝 (everlearning.sen.go.kr)
    SEOUL_LLL,  // 서울시 평생학습포털
    KURIQ_TEST  // 테스트용 플랫폼
}
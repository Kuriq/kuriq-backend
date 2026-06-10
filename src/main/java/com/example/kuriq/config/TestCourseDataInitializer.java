package com.example.kuriq.config;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.entity.roadmap.Platform;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TestCourseDataInitializer {

    private final CourseRepository courseRepository;

    // 테스트 강좌 ID 상수 — AiClient에서 참조
    public static final String PYTHON_BASIC = "aaaaaaaa-0000-0000-0000-000000000001";
    public static final String PYTHON_PROG = "aaaaaaaa-0000-0000-0000-000000000002";
    public static final String DATA_SCIENCE = "aaaaaaaa-0000-0000-0000-000000000003";
    public static final String PANDAS_BASIC = "aaaaaaaa-0000-0000-0000-000000000004";
    public static final String DATA_VIZ = "aaaaaaaa-0000-0000-0000-000000000005";
    public static final String ML_BASIC = "aaaaaaaa-0000-0000-0000-000000000006";
    public static final String SQL_BASIC = "aaaaaaaa-0000-0000-0000-000000000007";
    public static final String STATISTICS = "aaaaaaaa-0000-0000-0000-000000000008";

    @Bean
    public CommandLineRunner seedTestCourses() {
        return args -> {
            List<Course> courses = List.of(
                    Course.builder()
                            .id(PYTHON_BASIC)
                            .platform(Platform.K_MOOC)
                            .platformCourseId("KMOOC-PY-001")
                            .title("모두를 위한 파이썬")
                            .institution("서울대학교")
                            .category("프로그래밍")
                            .difficulty("입문")
                            .durationWeeks(4)
                            .estimatedHours(BigDecimal.valueOf(12.0))
                            .hasCertificate(true)
                            .url("https://www.kmooc.kr/courses/python-basic")
                            .description("파이썬 기초 문법과 프로그래밍 사고를 배웁니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(PYTHON_PROG)
                            .platform(Platform.KOCW)
                            .platformCourseId("KOCW-PY-002")
                            .title("파이썬으로 시작하는 프로그래밍")
                            .institution("고려대학교")
                            .category("프로그래밍")
                            .difficulty("초급")
                            .durationWeeks(6)
                            .estimatedHours(BigDecimal.valueOf(18.0))
                            .hasCertificate(true)
                            .url("https://www.kocw.net/courses/python-programming")
                            .description("파이썬을 활용한 프로그래밍 기초를 다집니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(DATA_SCIENCE)
                            .platform(Platform.K_MOOC)
                            .platformCourseId("KMOOC-DS-001")
                            .title("데이터 과학을 위한 파이썬 입문")
                            .institution("KAIST")
                            .category("데이터 분석")
                            .difficulty("초급")
                            .durationWeeks(4)
                            .estimatedHours(BigDecimal.valueOf(10.0))
                            .hasCertificate(true)
                            .url("https://www.kmooc.kr/courses/data-science-python")
                            .description("데이터 과학의 기초 개념과 파이썬 활용법을 배웁니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(PANDAS_BASIC)
                            .platform(Platform.KOCW)
                            .platformCourseId("KOCW-PD-001")
                            .title("Pandas 기초와 데이터 전처리")
                            .institution("연세대학교")
                            .category("데이터 분석")
                            .difficulty("초급")
                            .durationWeeks(3)
                            .estimatedHours(BigDecimal.valueOf(8.0))
                            .hasCertificate(false)
                            .url("https://www.kocw.net/courses/pandas-basics")
                            .description("Pandas를 활용한 데이터 전처리 방법을 학습합니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(DATA_VIZ)
                            .platform(Platform.K_MOOC)
                            .platformCourseId("KMOOC-DV-001")
                            .title("Python으로 배우는 데이터 시각화")
                            .institution("POSTECH")
                            .category("데이터 분석")
                            .difficulty("초급")
                            .durationWeeks(4)
                            .estimatedHours(BigDecimal.valueOf(10.0))
                            .hasCertificate(true)
                            .url("https://www.kmooc.kr/courses/data-visualization")
                            .description("matplotlib와 seaborn을 활용한 데이터 시각화를 배웁니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(ML_BASIC)
                            .platform(Platform.K_MOOC)
                            .platformCourseId("KMOOC-ML-001")
                            .title("머신러닝 입문")
                            .institution("서울대학교")
                            .category("인공지능")
                            .difficulty("중급")
                            .durationWeeks(8)
                            .estimatedHours(BigDecimal.valueOf(24.0))
                            .hasCertificate(true)
                            .url("https://www.kmooc.kr/courses/machine-learning")
                            .description("머신러닝의 기본 개념과 알고리즘을 학습합니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(SQL_BASIC)
                            .platform(Platform.KOCW)
                            .platformCourseId("KOCW-SQL-001")
                            .title("SQL로 시작하는 데이터베이스")
                            .institution("한양대학교")
                            .category("데이터베이스")
                            .difficulty("입문")
                            .durationWeeks(4)
                            .estimatedHours(BigDecimal.valueOf(10.0))
                            .hasCertificate(false)
                            .url("https://www.kocw.net/courses/sql-database")
                            .description("SQL 기초와 데이터베이스 활용법을 배웁니다.")
                            .isActive(true)
                            .build(),
                    Course.builder()
                            .id(STATISTICS)
                            .platform(Platform.K_MOOC)
                            .platformCourseId("KMOOC-ST-001")
                            .title("데이터 분석을 위한 통계학 기초")
                            .institution("고려대학교")
                            .category("통계학")
                            .difficulty("입문")
                            .durationWeeks(6)
                            .estimatedHours(BigDecimal.valueOf(15.0))
                            .hasCertificate(true)
                            .url("https://www.kmooc.kr/courses/statistics-basics")
                            .description("데이터 분석에 필요한 통계학 기초를 학습합니다.")
                            .isActive(true)
                            .build()
            );

            courses.forEach(course -> {
                // platform + platformCourseId로 중복 체크
                if (courseRepository.findByPlatformAndPlatformCourseId(
                        course.getPlatform(), course.getPlatformCourseId()).isEmpty()) {
                    courseRepository.save(course);
                }
            });
        };
    }
}

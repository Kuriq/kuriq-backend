package com.example.kuriq.config;

import com.example.kuriq.entity.roadmap.Course;
import com.example.kuriq.repository.roadmap.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@RequiredArgsConstructor
public class TestCourseDataInitializer {

    public static final String TEST_COURSE_ID = "11111111-1111-1111-1111-111111111111";

    private final CourseRepository courseRepository;

    @Bean
    public CommandLineRunner seedTestCourse() {
        return args -> {
            String platform = "kuriq-test";
            String platformCourseId = "test-python-001";

            if (courseRepository.existsById(TEST_COURSE_ID)
                    || courseRepository.findByPlatformAndPlatformCourseId(platform, platformCourseId).isPresent()) {
                return;
            }

            Course course = Course.builder()
                    .id(TEST_COURSE_ID)
                    .platform(platform)
                    .platformCourseId(platformCourseId)
                    .title("모두를 위한 파이썬")
                    .institution("Kuriq Test Lab")
                    .category("프로그래밍")
                    .difficulty("초급")
                    .durationWeeks(4)
                    .estimatedHours(BigDecimal.valueOf(12.0))
                    .hasCertificate(false)
                    .url("https://example.com/courses/test-python-001")
                    .description("노트 생성과 퀴즈/채팅 테스트를 위한 임시 파이썬 강좌입니다.")
                    .isActive(true)
                    .build();

            courseRepository.save(course);
        };
    }
}

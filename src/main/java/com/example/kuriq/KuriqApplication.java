package com.example.kuriq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync   // @Async 동작을 위해 추가 (CourseClickEventListener에서 사용)
@EnableScheduling   // 스케줄러(@Scheduled) 동작을 위해 필요
@SpringBootApplication
public class KuriqApplication {
    public static void main(String[] args) {
        SpringApplication.run(KuriqApplication.class, args);
    }
}
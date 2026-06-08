package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.User;  // 이 Repository가 다룰 엔티티
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// User 테이블을 조회/저장하는 인터페이스
public interface UserRepository extends JpaRepository<User, String> { // interface 선언 -> jpa가 자동으로 구현 클래스 생성해줌
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    Optional<User> findByEmail(String email);  // 회원가입 시 softDelete된 계정 재활성화를 위해 이메일 찾기

    // 이메일 알림 활성화된 활성 로드맵 있는 사용자 조회
    @Query("SELECT u FROM User u JOIN NotificationSetting ns ON ns.userId = u.id " +
            "WHERE u.isDeleted = false AND ns.emailEnabled = true AND ns.weeklyStartAlert = true")
    List<User> findUsersWithEmailAndWeeklyAlert();
}

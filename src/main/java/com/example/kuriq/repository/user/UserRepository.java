package com.example.kuriq.repository.user;

import com.example.kuriq.entity.user.User;  // 이 Repository가 다룰 엔티티
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// User 테이블을 조회/저장하는 인터페이스
public interface UserRepository extends JpaRepository<User, String> { // interface 선언 -> jpa가 자동으로 구현 클래스 생성해줌
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);
}

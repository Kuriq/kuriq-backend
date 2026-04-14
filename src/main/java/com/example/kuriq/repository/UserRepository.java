package com.example.kuriq.repository;

import com.example.kuriq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> { // interface 선언 -> jpa가 자동으로 구현 클래스 생성해줌
}

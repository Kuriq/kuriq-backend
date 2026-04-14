package com.example.kuriq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // 이 클래스는 DB 클래스다 => User class <-> User table
@Getter // 모든 필드에 대해 GetEmail(), GetName() 등등 자동 생성
@NoArgsConstructor  // 기본생성자 추가(JPA는 기본 생성자 필수임)
@AllArgsConstructor // 클래스 내부에 선언된 모든 filed에 각 파라미터를 가진 생성자를 생성
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) // 이 필드는 기본키임, 자동으로 숫자 증가시키면서 db에 값 넣음
    private Long id;

    private String email;
    private String password;
    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;
}

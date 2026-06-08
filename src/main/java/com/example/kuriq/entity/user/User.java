package com.example.kuriq.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;


/**
 * ERD: users 테이블
 *
 * 자체 가입(LOCAL)과 소셜 가입을 하나의 테이블에서 관리한다.
 * 소셜 전용 계정은 password가 NULL이고, social_accounts 테이블에 프로바이더 정보가 저장된다.
 * 소프트 삭제를 사용하므로 실제 데이터는 지우지 않는다(is_deleted = true).
 */

@Entity // 이 클래스는 DB 클래스다 => User class <-> User table
@Table(
        name = "users", // 테이블과 클래스 이름이 달라서 @Table 어노테이션 필요
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                // @Index(name = "idx_users_provider_deleted", columnList = "authProvider, isDeleted") 이거 나중에 필요하면 다시 추가
        }
)
@Getter // 모든 필드에 대해 GetEmail(), GetName() 등등 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 기본생성자 추가(JPA는 기본 생성자 필수임)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    // 이메일. 카카오가 이메일을 미제공하면 NULL이 될 수 있음

    @Column(unique = true, length = 255)
    private String email;

    // bcrypt 해시. 소셜 전용 계정은 NULL
    @Column(length = 255)
    private String password;

    // nullable: null을 가질 수 업는 값 형식에 null을 할당할 수 있도록 허용하는 기능
    @Column(nullable = false, length = 20)  // null 할당 안한다는 의미
    private String name;

    // 사용자 연령층
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AgeGroup ageGroup;

    // 이 회원이 일반 회원가입인지, 구글인지, 카카오인지 등등을 구분하는 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuthProvider authProvider = AuthProvider.LOCAL; // 기본값 일반 회원가입으로 설정

    // 실제 삭제 대신 논리 삭제용
    @Column(nullable = false)
    private Boolean isDeleted = false;

    // enum: 서로 연관된 상수들의 집합
    public enum AgeGroup {
        TWENTIES, THIRTIES, FORTIES, FIFTIES, SIXTIES_OR_ABOVE
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, NAVER, KAKAO // authProvider 변수에 이 4개 중 하나만 들어갈 수 있음
    }

    // 논리삭제
    public void softDelete() {
        this.isDeleted = true;
        /** 회원 탈퇴하고 재가입할 때 이메일 중복이면 재가입 안되는 문제 해결 */
        this.email = "deleted_" + this.id + "@deleted.com";  // 이메일 익명화
        this.name = "탈퇴한 사용자";  // "탈퇴한 사용자"로 표시되게
        // 탈퇴한 계정으로 로그인 못 하게 이중 보안(이미 findByEmailAndIsDeletedFalse로 막혀있긴함)
        this.password = null;
    }

    // 소프트 삭제 시 삭제된 시간 저장 (NULL이면 삭제되지 않은 상태)
    private LocalDateTime deletedAt;

    // 생성 시간 (최초 생성 시에만 값이 들어가고 이후 수정 불가)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시간 (데이터가 수정될 때마다 갱신됨)
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 엔티티가 처음 저장되기 직전에 자동 실행됨
    // createdAt, updatedAt을 현재 시간으로 설정
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // 엔티티가 업데이트되기 직전에 자동 실행됨
    // updatedAt만 현재 시간으로 갱신
    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 회원가입 시 softDelete된 계정이면 재활성화
    public void reactivate(String encodedPassword, String name) {
        this.password = encodedPassword;
        this.name = name;
        this.isDeleted = false;
        this.deletedAt = null;
    }

    // 비밀번호 변경
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}

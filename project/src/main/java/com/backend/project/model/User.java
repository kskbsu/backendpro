package com.backend.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users") // 데이터베이스 테이블 이름을 'users'로 지정
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 ID를 자동으로 생성하도록 설정
    private Long id;

    @Column(nullable = false, unique = true) // null을 허용하지 않고, 유일한 값이어야 함
    private String username; // 로그인 ID

    @Column(nullable = false)
    private String password; // 암호화된 비밀번호 저장

    @Column(nullable = false, unique = true)
    private String nickname; // 채팅에서 사용할 닉네임

    // preferredLanguage 필드 제거
}
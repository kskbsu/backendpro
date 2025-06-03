package com.backend.project.repository;

import com.backend.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 사용자 이름(username)으로 사용자를 찾는 메소드 (로그인 시 사용)
    Optional<User> findByUsername(String username);
}
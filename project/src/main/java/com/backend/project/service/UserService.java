package com.backend.project.service;

import com.backend.project.dto.SignupRequestDto;
import com.backend.project.model.User;
import com.backend.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // 추가


import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String nickname = requestDto.getNickname();

        // 사용자명 중복 확인
        Optional<User> checkUsername = userRepository.findByUsername(username);
        if (checkUsername.isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 사용자명입니다: " + username);
        }

        // 닉네임 중복 확인 (User 엔티티의 nickname 필드에 unique=true 제약조건이 있으므로, DB 레벨에서도 체크됨)
        // Optional<User> checkNickname = userRepository.findByNickname(nickname); // UserRepository에 findByNickname 메소드 추가 필요
        // if (checkNickname.isPresent()) {
        //     throw new IllegalArgumentException("이미 사용 중인 닉네임입니다: " + nickname);
        // }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        User user = User.builder()
                .username(username)
                .password(encodedPassword)
                .nickname(nickname)
                // .preferredLanguage(requestDto.getPreferredLanguage()) // 선호 언어 설정 제거
                .build();

        return userRepository.save(user);
    }
    // username으로 User 엔티티를 조회하는 메소드
    @Transactional(readOnly = true) // 읽기 전용 트랜잭션
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }
}

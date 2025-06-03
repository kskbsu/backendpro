package com.backend.project.controller;

import com.backend.project.dto.SignupRequestDto;
import com.backend.project.model.User;
import com.backend.project.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequestDto signupRequestDto) {
        try {
            log.info("Attempting to register user: {}", signupRequestDto.getUsername());
            userService.registerUser(signupRequestDto);
            log.info("User {} registered successfully", signupRequestDto.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
        } catch (IllegalArgumentException e) {
            log.warn("Signup failed for user {}: {}", signupRequestDto.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during signup for user {}: {}", signupRequestDto.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 예상치 못한 오류가 발생했습니다.");
        }
    }

 @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            log.info("No authenticated user found or user is anonymous.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        String username;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        log.info("Fetching details for current authenticated user: {}", username);
        User user = userService.getUserByUsername(username); // UserService에 getUserByUsername 메소드 추가 필요

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("nickname", user.getNickname());
        return ResponseEntity.ok(userInfo);
    }
}
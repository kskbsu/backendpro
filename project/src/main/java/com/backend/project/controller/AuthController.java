package com.backend.project.controller;

import com.backend.project.dto.SignupRequestDto;
import com.backend.project.model.User;
import com.backend.project.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
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

import com.backend.project.exception.ApiException;
import com.backend.project.exception.ErrorCode;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 회원가입 처리.
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {
        log.info("Attempting to register user: {}", signupRequestDto.getUsername());
        userService.registerUser(signupRequestDto);
        log.info("User {} registered successfully", signupRequestDto.getUsername());
        Map<String, String> body = new HashMap<>();
        body.put("message", "회원가입 성공");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // 현재 로그인 사용자 조회.
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            log.info("No authenticated user found or user is anonymous.");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        String username;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        log.info("Fetching details for current authenticated user: {}", username);
        User user = userService.getUserByUsername(username);

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("nickname", user.getNickname());
        return ResponseEntity.ok(userInfo);
    }
}
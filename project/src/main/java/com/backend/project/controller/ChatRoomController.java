package com.backend.project.controller;

import com.backend.project.dto.ChatRoomDTO;
// import com.backend.project.model.User; // 현재 미사용
import com.backend.project.service.ChatRoomService;
// import com.backend.project.service.UserService; // 현재 미사용
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder; // 제거 또는 주석 처리
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.security.core.userdetails.UserDetails; // 제거 또는 주석 처리
// import org.springframework.web.bind.annotation.GetMapping; // 제거 또는 주석 처리
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    // private final UserService userService; // 현재 getAllChatRooms에서는 직접 사용하지 않음

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getAllChatRooms(Authentication authentication) {
        // 인증된 사용자인지 확인 (선택적: 공개 채팅방 목록만 제공한다면 인증이 필수는 아닐 수 있음)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            log.warn("Attempt to access chat rooms by unauthenticated user.");
            // 상황에 따라 401 Unauthorized 또는 빈 목록을 반환할 수 있습니다.
            // 여기서는 인증된 사용자만 접근 가능하다고 가정합니다.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("User {} requesting all chat rooms", authentication.getName());
        return ResponseEntity.ok(chatRoomService.getAllChatRooms());
    }
}
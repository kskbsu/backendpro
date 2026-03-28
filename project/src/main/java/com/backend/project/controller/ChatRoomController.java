package com.backend.project.controller;

import com.backend.project.dto.ChatRoomDTO;
import com.backend.project.exception.ApiException;
import com.backend.project.exception.ErrorCode;
import com.backend.project.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 목록 조회.
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getAllChatRooms(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            log.warn("Attempt to access chat rooms by unauthenticated user.");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        log.info("User {} requesting all chat rooms", authentication.getName());
        return ResponseEntity.ok(chatRoomService.getAllChatRooms());
    }
}
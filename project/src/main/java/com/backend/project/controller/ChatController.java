package com.backend.project.controller;

import com.backend.project.dto.ChatMessageDTO;
import com.backend.project.model.User;
import com.backend.project.service.ChatMessageService;
import com.backend.project.service.ChatRoomService;
import com.backend.project.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;

    public enum MessageType {
        CHAT, JOIN, LEAVE, HISTORY
    }

    // 클라이언트 요청 페이로드.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientMessagePayload {
        private String content;
        private String sender;
    }

    // 서버 브로드캐스트/개별 전송 페이로드.
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastMessage {
        private String roomId;
        private String sender;
        private String content;
        private MessageType type;
        private LocalDateTime timestamp;
        private List<ChatMessageDTO> history;
    }

    // JOIN/LEAVE 표시 닉네임 결정.
    private String determineNicknameForEvent(SimpMessageHeaderAccessor headerAccessor, ClientMessagePayload clientPayload, String eventType) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        String resolvedNickname = "Unknown";
        String authUsername = null;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            authUsername = authentication.getName();
            try {
                User currentUser = userService.getUserByUsername(authUsername);
                resolvedNickname = currentUser.getNickname();
                log.info("Nickname for {} event for authenticated user '{}' resolved from DB: {}", eventType, authUsername, resolvedNickname);
                return resolvedNickname;
            } catch (Exception e) {
                log.error("Error fetching nickname from DB for authenticated user '{}' for {} event: {}. Falling back to client payload or default.", authUsername, eventType, e.getMessage());
            }
        }

        // 인증 닉네임 조회 실패 시 payload.sender 사용.
        if (clientPayload != null && clientPayload.getSender() != null && !clientPayload.getSender().isEmpty()) {
            resolvedNickname = clientPayload.getSender();
            log.warn("Nickname for {} event resolved from client payload: {}. (Authenticated user: {})", eventType, resolvedNickname, authUsername != null ? authUsername : "N/A or Anonymous");
        } else {
            log.warn("Nickname for {} event could not be resolved from client payload (payload or sender missing). Defaulting to 'Unknown'. (Authenticated user: {})", eventType, authUsername != null ? authUsername : "N/A or Anonymous");
        }
        return resolvedNickname;
    }

    // 메시지 수신 -> 저장 -> 브로드캐스트.
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        if (clientPayload == null || clientPayload.getContent() == null) {
            log.warn("Received empty chat payload for room {}. Ignoring message.", roomId);
            return;
        }

        Authentication authentication = (Authentication) headerAccessor.getUser();
        String currentUsername = "Unknown";
        String currentNickname = "Unknown";

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            currentUsername = authentication.getName();
            try {
                User currentUser = userService.getUserByUsername(currentUsername);
                currentNickname = currentUser.getNickname();
            } catch (Exception e) {
                log.error("Error fetching user details for sender: {} - {}", currentUsername, e.getMessage(), e);
            }
        } else {
            log.warn("Attempt to send message from unauthenticated or anonymous user. Username: {}, Payload Sender: {}",
                    currentUsername, (clientPayload != null ? clientPayload.getSender() : "N/A"));
        }


        com.backend.project.model.ChatMessage savedMessage = chatMessageService.saveMessage(roomId, currentNickname, clientPayload.getContent());

        BroadcastMessage broadcastMessage = new BroadcastMessage(
                roomId,
                savedMessage.getSenderNickname(),
                savedMessage.getContent(),
                MessageType.CHAT,
                savedMessage.getTimestamp(),
                null
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, broadcastMessage);
    }

    // 유저 입장 처리(JOIN + 히스토리 전송).
    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        String nicknameToUse = determineNicknameForEvent(headerAccessor, clientPayload, "JOIN");
        
        Authentication authentication = (Authentication) headerAccessor.getUser();
        String authUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) 
                                ? authentication.getName() : "N/A_or_Anonymous";

        log.info("User (Auth Username: {}) with resolved nickname '{}' joining room '{}'. Client payload sender: '{}'",
                 authUsername, nicknameToUse, roomId, (clientPayload != null ? clientPayload.getSender() : "N/A"));

        if (!"N/A_or_Anonymous".equals(authUsername)) {
            try {
                User user = userService.getUserByUsername(authUsername);
                chatRoomService.joinRoom(roomId, user);
            } catch (Exception e) {
                log.error("joinRoom failed for user '{}' roomId={}: {}", authUsername, roomId, e.getMessage());
            }
        }

        BroadcastMessage joinMessage = new BroadcastMessage(roomId, nicknameToUse, null, MessageType.JOIN, LocalDateTime.now(), null);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, joinMessage);
        log.info("Sent JOIN message to /topic/room/{} for sender '{}'", roomId, nicknameToUse);

        List<com.backend.project.model.ChatMessage> historyEntities = chatMessageService.getMessagesForRoom(roomId);
        List<ChatMessageDTO> historyDTOs = historyEntities.stream()
                .map(msg -> new ChatMessageDTO(msg.getSenderNickname(), msg.getContent(), msg.getTimestamp()))
                .collect(Collectors.toList());
        log.info("Fetched {} history messages for room {}, converted to {} DTOs", historyEntities.size(), roomId, historyDTOs.size());
        
        if (authUsername != null && !authUsername.equals("N/A_or_Anonymous")) {
            log.info("Attempting to send history ({} DTOs) to user '{}' at /queue/room.history for room {}", 
                     historyDTOs.size(), authUsername, roomId);
             messagingTemplate.convertAndSendToUser(
                authUsername,
                "/queue/room.history",
                new BroadcastMessage(roomId, null, null, MessageType.HISTORY, LocalDateTime.now(), historyDTOs)
             );
        } else {
            log.warn("User (resolved nickname: '{}') is not properly authenticated (auth username: '{}') to receive history for room {}. History not sent.", 
                     nicknameToUse, authUsername, roomId);
        }
    }

    // 유저 퇴장 처리(LEAVE 브로드캐스트).
    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        String nicknameToUse = determineNicknameForEvent(headerAccessor, clientPayload, "LEAVE");

        Authentication authentication = (Authentication) headerAccessor.getUser();
        String authUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) 
                                ? authentication.getName() : "N/A_or_Anonymous";

        log.info("User (Auth Username: {}) with resolved nickname '{}' leaving room '{}'. Client payload sender: '{}'",
                 authUsername, nicknameToUse, roomId, (clientPayload != null ? clientPayload.getSender() : "N/A"));

        if (!"N/A_or_Anonymous".equals(authUsername)) {
            try {
                User user = userService.getUserByUsername(authUsername);
                chatRoomService.leaveRoom(roomId, user);
            } catch (Exception e) {
                log.error("leaveRoom failed for user '{}' roomId={}: {}", authUsername, roomId, e.getMessage());
            }
        }

        BroadcastMessage leaveMessage = new BroadcastMessage(roomId, nicknameToUse, null, MessageType.LEAVE, LocalDateTime.now(), null);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);
        log.info("Sent LEAVE message to /topic/room/{} for sender '{}'", roomId, nicknameToUse);
    }
}

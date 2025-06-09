package com.backend.project.controller;

import com.backend.project.dto.ChatMessageDTO; // ChatMessageDTO import
// import com.backend.project.model.ChatMessage; // 이전 DTO import, 현재는 내부 DTO 또는 엔티티 사용
import com.backend.project.model.User; // User 모델 import
import com.backend.project.service.ChatMessageService; // ChatMessageService import
import com.backend.project.service.UserService; // UserService import
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j; // Slf4j import 추가
import org.springframework.stereotype.Controller;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j // Slf4j 어노테이션 추가
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserService userService; // UserService 주입
    private final ChatMessageService chatMessageService; // ChatMessageService 주입

    // 메시지 타입을 위한 Enum
    public enum MessageType {
        CHAT, JOIN, LEAVE, HISTORY
    }

    // 클라이언트에서 받는 메시지 페이로드 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientMessagePayload {
        private String content;
        private String sender; // JOIN, LEAVE 시 클라이언트가 닉네임을 보낼 수 있음 (인증 안된 경우 대비)
    }

    // 클라이언트로 브로드캐스팅하거나 전송할 메시지 DTO
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
        private List<ChatMessageDTO> history; // 이전 대화 내역 전송용 (ChatMessageDTO 리스트)
    }

    /**
     * JOIN 또는 LEAVE 이벤트에 사용될 사용자 닉네임을 결정합니다.
     * 1. 인증된 사용자의 경우, DB에서 닉네임을 조회합니다.
     * 2. 인증 정보가 없거나 DB 조회에 실패한 경우, 클라이언트 페이로드의 sender 값을 사용합니다.
     * 3. 모든 정보가 없는 경우 "Unknown"을 반환합니다.
     * @param headerAccessor 메시지 헤더 접근자
     * @param clientPayload 클라이언트 페이로드 (sender 닉네임 포함 가능)
     * @param eventType 이벤트 타입 문자열 (로깅용, 예: "JOIN", "LEAVE")
     * @return 결정된 사용자 닉네임
     */
    private String determineNicknameForEvent(SimpMessageHeaderAccessor headerAccessor, ClientMessagePayload clientPayload, String eventType) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        String resolvedNickname = "Unknown"; // 기본값
        String authUsername = null;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            authUsername = authentication.getName();
            try {
                User currentUser = userService.getUserByUsername(authUsername);
                resolvedNickname = currentUser.getNickname();
                log.info("Nickname for {} event for authenticated user '{}' resolved from DB: {}", eventType, authUsername, resolvedNickname);
                return resolvedNickname; // 인증된 사용자의 DB 닉네임 사용
            } catch (Exception e) {
                log.error("Error fetching nickname from DB for authenticated user '{}' for {} event: {}. Falling back to client payload or default.", authUsername, eventType, e.getMessage());
                // resolvedNickname은 "Unknown"으로 유지, 아래 로직에서 clientPayload.getSender() 시도
            }
        }

        // 인증되지 않았거나, 인증되었지만 DB에서 닉네임 조회 실패 시 클라이언트 페이로드의 sender 사용
        if (clientPayload != null && clientPayload.getSender() != null && !clientPayload.getSender().isEmpty()) {
            resolvedNickname = clientPayload.getSender();
            log.warn("Nickname for {} event resolved from client payload: {}. (Authenticated user: {})", eventType, resolvedNickname, authUsername != null ? authUsername : "N/A or Anonymous");
        } else {
            log.warn("Nickname for {} event could not be resolved from client payload (payload or sender missing). Defaulting to 'Unknown'. (Authenticated user: {})", eventType, authUsername != null ? authUsername : "N/A or Anonymous");
        }
        return resolvedNickname;
    }

    /**
     * /app/chat.sendMessage/{roomId} 로 메시지가 오면 이 메소드가 실행됩니다.
     * 해당 roomId의 토픽(/topic/room/{roomId})으로 메시지를 발행합니다.
     * @param roomId 채팅방 ID
     * @param clientPayload 클라이언트가 보낸 메시지 내용
     * @param headerAccessor WebSocket 메시지 헤더 접근자
     */
    @MessageMapping("/chat.sendMessage/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        Authentication authentication = (Authentication) headerAccessor.getUser();
        String currentUsername = "Unknown"; // 기본값
        String currentNickname = "Unknown";

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            currentUsername = authentication.getName(); // 로그인 ID (username)
            try {
                User currentUser = userService.getUserByUsername(currentUsername); // DB에서 User 엔티티 조회
                currentNickname = currentUser.getNickname(); // 실제 닉네임 사용
            } catch (Exception e) {
                log.error("Error fetching user details for sender: {} - {}", currentUsername, e.getMessage(), e);
            }
        } else {
            // 인증되지 않은 사용자가 메시지를 보내는 경우, 페이로드의 sender를 사용하거나 "Unknown"으로 처리
            // 현재 로직에서는 인증된 사용자만 메시지를 보낸다고 가정하고, currentNickname이 "Unknown"으로 남을 수 있음
            // 필요하다면 clientPayload.getSender()를 여기서도 활용할 수 있으나, CHAT 메시지는 보통 인증된 사용자가 보냄
            log.warn("Attempt to send message from unauthenticated or anonymous user. Username: {}, Payload Sender: {}",
                    currentUsername, (clientPayload != null ? clientPayload.getSender() : "N/A"));
            // 만약 인증되지 않은 사용자의 메시지 전송을 허용하고, 그 닉네임을 사용하려면:
            // if (clientPayload != null && clientPayload.getSender() != null && !clientPayload.getSender().isEmpty()) {
            //     currentNickname = clientPayload.getSender();
            // }
        }


        // 메시지 저장
        com.backend.project.model.ChatMessage savedMessage = chatMessageService.saveMessage(roomId, currentNickname, clientPayload.getContent());

        // 브로드캐스팅할 메시지 DTO 생성
        BroadcastMessage broadcastMessage = new BroadcastMessage(
                roomId,
                savedMessage.getSenderNickname(), // 저장된 메시지의 닉네임 사용 (DB에서 온 값 또는 결정된 값)
                savedMessage.getContent(),
                MessageType.CHAT,
                savedMessage.getTimestamp(),
                null // CHAT 메시지에는 history 불필요
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId, broadcastMessage);
    }

    /**
     * /app/chat.addUser/{roomId} 로 메시지가 오면 이 메소드가 실행됩니다.
     * 새로운 사용자가 특정 채팅방에 참여했을 때 JOIN 메시지를 해당 방의 토픽으로 보냅니다.
     * 또한, 이전 대화 내용을 참여한 사용자에게만 전송합니다.
     * @param roomId 채팅방 ID
     * @param clientPayload 클라이언트가 보낸 페이로드 (sender 닉네임 포함 가능)
     * @param headerAccessor WebSocket 메시지 헤더 접근자
     */
    @MessageMapping("/chat.addUser/{roomId}")
    public void addUser(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        String nicknameToUse = determineNicknameForEvent(headerAccessor, clientPayload, "JOIN");
        
        Authentication authentication = (Authentication) headerAccessor.getUser();
        String authUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) 
                                ? authentication.getName() : "N/A_or_Anonymous";

        log.info("User (Auth Username: {}) with resolved nickname '{}' joining room '{}'. Client payload sender: '{}'", 
                 authUsername, nicknameToUse, roomId, (clientPayload != null ? clientPayload.getSender() : "N/A"));
        
        // JOIN 메시지 브로드캐스팅
        BroadcastMessage joinMessage = new BroadcastMessage(roomId, nicknameToUse, null, MessageType.JOIN, LocalDateTime.now(), null);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, joinMessage);
        log.info("Sent JOIN message to /topic/room/{} for sender '{}'", roomId, nicknameToUse);

        // 이전 대화 내용 가져오기 (ChatMessage 엔티티 리스트)
        List<com.backend.project.model.ChatMessage> historyEntities = chatMessageService.getMessagesForRoom(roomId);
        // ChatMessage 엔티티 리스트를 ChatMessageDTO 리스트로 변환
        List<ChatMessageDTO> historyDTOs = historyEntities.stream()
                .map(msg -> new ChatMessageDTO(msg.getSenderNickname(), msg.getContent(), msg.getTimestamp()))
                .collect(Collectors.toList());
        log.info("Fetched {} history messages for room {}, converted to {} DTOs", historyEntities.size(), roomId, historyDTOs.size());
        
        // 참여한 사용자에게만 이전 대화 내용 전송
        if (authUsername != null && !authUsername.equals("N/A_or_Anonymous")) { // 인증된 사용자에게만 전송
            log.info("Attempting to send history ({} DTOs) to user '{}' at /queue/room.history for room {}", 
                     historyDTOs.size(), authUsername, roomId);
             messagingTemplate.convertAndSendToUser(
                authUsername, // Spring Security Principal name (username)
                "/queue/room.history", // 클라이언트 구독 경로: /user/queue/room.history
                new BroadcastMessage(roomId, null, null, MessageType.HISTORY, LocalDateTime.now(), historyDTOs) // DTO 리스트를 포함한 메시지
             );
        } else {
            log.warn("User (resolved nickname: '{}') is not properly authenticated (auth username: '{}') to receive history for room {}. History not sent.", 
                     nicknameToUse, authUsername, roomId);
        }
    }

    /**
     * /app/chat.leaveUser/{roomId} 로 메시지가 오면 이 메소드가 실행됩니다.
     * 사용자가 특정 채팅방에서 퇴장했을 때 LEAVE 메시지를 해당 방의 토픽으로 보냅니다.
     * 이 방식은 클라이언트가 명시적으로 퇴장 메시지를 보낼 때 사용됩니다.
     * WebSocket 세션 종료 시 자동으로 처리하려면 SessionDisconnectEvent 리스너를 사용하는 것이 더 좋습니다.
     * @param roomId 채팅방 ID
     * @param clientPayload 클라이언트가 보낸 페이로드 (sender 닉네임 포함 가능)
     * @param headerAccessor WebSocket 메시지 헤더 접근자
     */
    @MessageMapping("/chat.leaveUser/{roomId}")
    public void leaveUser(@DestinationVariable String roomId, @Payload ClientMessagePayload clientPayload, SimpMessageHeaderAccessor headerAccessor) {
        String nicknameToUse = determineNicknameForEvent(headerAccessor, clientPayload, "LEAVE");

        Authentication authentication = (Authentication) headerAccessor.getUser();
        String authUsername = (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) 
                                ? authentication.getName() : "N/A_or_Anonymous";

        log.info("User (Auth Username: {}) with resolved nickname '{}' leaving room '{}'. Client payload sender: '{}'", 
                 authUsername, nicknameToUse, roomId, (clientPayload != null ? clientPayload.getSender() : "N/A"));

        // LEAVE 메시지 브로드캐스팅
        BroadcastMessage leaveMessage = new BroadcastMessage(roomId, nicknameToUse, null, MessageType.LEAVE, LocalDateTime.now(), null);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMessage);
        log.info("Sent LEAVE message to /topic/room/{} for sender '{}'", roomId, nicknameToUse);
    }
}

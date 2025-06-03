package com.backend.project.service;

import com.backend.project.model.ChatMessage;
import com.backend.project.model.ChatRoom;
import com.backend.project.repository.ChatMessageRepository;
import com.backend.project.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatMessage saveMessage(String roomId, String senderNickname, String content) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseGet(() -> {
                    // 방이 존재하지 않으면 새로 생성 (또는 예외 처리)
                    // 여기서는 간단히 임시 방을 만들거나, 실제로는 예외를 던져야 할 수 있습니다.
                    // 혹은, 방은 항상 미리 존재한다고 가정할 수도 있습니다. (현재는 자동 생성)
                    // 현재 구조에서는 roomId로 입장하므로, 방이 없다면 문제가 될 수 있습니다.
                    log.warn("Chat room with roomId {} not found. Creating new room for message from {}.", roomId, senderNickname);
                    // throw new RuntimeException("Chat room not found: " + roomId);
                    // 임시 방 생성 로직 (선택적, 실제 서비스에서는 더 견고한 처리가 필요)
                    ChatRoom newRoom = ChatRoom.builder().roomId(roomId).name("Room " + roomId).type(ChatRoom.ChatRoomType.GROUP).build();
                    ChatRoom savedNewRoom = chatRoomRepository.save(newRoom);
                    log.info("New chat room created with id: {}, roomId: {}", savedNewRoom.getId(), savedNewRoom.getRoomId());
                    return savedNewRoom;
                });

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderNickname(senderNickname)
                .content(content)
                // .timestamp(LocalDateTime.now()) // @PrePersist로 자동 설정됨
                .build();
        ChatMessage savedMsg = chatMessageRepository.save(chatMessage);
        log.info("Message saved: id={}, roomId={}, sender={}, content='{}'", savedMsg.getId(), chatRoom.getRoomId(), savedMsg.getSenderNickname(), savedMsg.getContent().substring(0, Math.min(savedMsg.getContent().length(), 20)));
        return savedMsg;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesForRoom(String roomId) {
        log.info("Attempting to fetch messages for roomId: {}", roomId);
        Optional<ChatRoom> roomOpt = chatRoomRepository.findByRoomId(roomId);
        if (roomOpt.isPresent()) {
            ChatRoom chatRoom = roomOpt.get();
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
            log.info("Found {} messages for room {} (DB id: {})", messages.size(), roomId, chatRoom.getId());
            return messages;
        } else {
            log.warn("ChatRoom not found for roomId: {} when fetching messages. Returning empty list.", roomId);
            return Collections.emptyList();
        }
    }
}
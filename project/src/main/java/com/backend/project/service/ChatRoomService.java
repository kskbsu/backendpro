package com.backend.project.service;

import com.backend.project.dto.ChatRoomDTO;
import com.backend.project.model.ChatRoom;
import com.backend.project.model.ChatRoomParticipant;
import com.backend.project.model.User;
import com.backend.project.repository.ChatRoomParticipantRepository;
import com.backend.project.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getAllChatRooms() {
        List<ChatRoom> chatRooms = chatRoomRepository.findAll();
        return chatRooms.stream()
                .map(ChatRoomDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"null", "NullableProblems", "ConstantConditions", "DataFlowIssue"})
    @Transactional
    public void joinRoom(String roomId, User user) {
        ChatRoom room = chatRoomRepository.findByRoomIdForUpdate(roomId).orElse(null);
        if (room == null) {
            log.warn("joinRoom skipped: no ChatRoom for roomId={}", roomId);
            return;
        }
        if (chatRoomParticipantRepository.existsByUserAndChatRoom(user, room)) {
            return;
        }
        chatRoomParticipantRepository.save(
                ChatRoomParticipant.builder().chatRoom(room).user(user).build());
        room.setParticipantCount(room.getParticipantCount() + 1);
        chatRoomRepository.save(room);
    }

    @SuppressWarnings({"null", "NullableProblems", "ConstantConditions", "DataFlowIssue"})
    @Transactional
    public void leaveRoom(String roomId, User user) {
        ChatRoom room = chatRoomRepository.findByRoomIdForUpdate(roomId).orElse(null);
        if (room == null) {
            log.warn("leaveRoom skipped: no ChatRoom for roomId={}", roomId);
            return;
        }
        if (!chatRoomParticipantRepository.existsByUserAndChatRoom(user, room)) {
            return;
        }
        chatRoomParticipantRepository.deleteByUserAndChatRoom(user, room);
        room.setParticipantCount(Math.max(0, room.getParticipantCount() - 1));
        chatRoomRepository.save(room);
    }
}
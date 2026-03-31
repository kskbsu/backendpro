package com.backend.project.service;

import com.backend.project.dto.ChatRoomDTO;
import com.backend.project.exception.ApiException;
import com.backend.project.exception.ErrorCode;
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

// 채팅방 참가·퇴장. 인원은 JPQL UPDATE로 갱신, 참가 행과 같은 트랜잭션.
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
        ChatRoom room = chatRoomRepository.findByRoomId(roomId).orElse(null);
        if (room == null) {
            log.warn("joinRoom skipped: no ChatRoom for roomId={}", roomId);
            return;
        }
        if (chatRoomParticipantRepository.existsByUserAndChatRoom(user, room)) {
            log.info("joinRoom skipped: already participant roomId={}, userId={}", roomId, user.getId());
            return;
        }
        int before = room.getParticipantCount();
        int updated = chatRoomRepository.incrementParticipantCountByRoomId(roomId);
        if (updated != 1) {
            log.info("joinRoom: room full or update skipped roomId={}, userId={}, beforeCount={}", roomId, user.getId(), before);
            throw new ApiException(ErrorCode.ROOM_FULL);
        }
        chatRoomParticipantRepository.save(
                ChatRoomParticipant.builder().chatRoom(room).user(user).build());
        log.info("joinRoom: roomId={}, userId={}, participantCount: {} -> {}", roomId, user.getId(), before, before + 1);
    }

    @SuppressWarnings({"null", "NullableProblems", "ConstantConditions", "DataFlowIssue"})
    @Transactional
    public void leaveRoom(String roomId, User user) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId).orElse(null);
        if (room == null) {
            log.warn("leaveRoom skipped: no ChatRoom for roomId={}", roomId);
            return;
        }
        if (!chatRoomParticipantRepository.existsByUserAndChatRoom(user, room)) {
            log.info("leaveRoom skipped: not a participant roomId={}, userId={}", roomId, user.getId());
            return;
        }
        int before = room.getParticipantCount();
        chatRoomParticipantRepository.deleteByUserAndChatRoom(user, room);
        int updated = chatRoomRepository.decrementParticipantCountByRoomId(roomId);
        int after = Math.max(0, before - 1);
        if (updated != 1 && before > 0) {
            log.warn("leaveRoom: unexpected decrement row count={} for roomId={}, userId={}, beforeCount={}",
                    updated, roomId, user.getId(), before);
        }
        log.info("leaveRoom: roomId={}, userId={}, participantCount: {} -> {}", roomId, user.getId(), before, after);
    }
}
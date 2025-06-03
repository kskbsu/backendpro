package com.backend.project.repository;

import com.backend.project.model.ChatRoom;
import com.backend.project.model.ChatRoomParticipant;
import com.backend.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    // 특정 사용자가 참여하고 있는 모든 ChatRoomParticipant 정보를 조회
    List<ChatRoomParticipant> findByUser(User user);

    // 특정 사용자와 특정 채팅방의 참여 정보를 조회 (예: 중복 참여 방지 등)
    Optional<ChatRoomParticipant> findByUserAndChatRoom(User user, ChatRoom chatRoom);
}
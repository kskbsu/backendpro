package com.backend.project.repository;

import com.backend.project.model.ChatMessage;
import com.backend.project.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 모든 메시지를 시간 순서대로 가져오는 메소드
    List<ChatMessage> findByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);
}
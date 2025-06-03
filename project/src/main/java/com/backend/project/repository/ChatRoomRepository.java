package com.backend.project.repository;

import com.backend.project.model.ChatRoom;
// import com.backend.project.model.User; // 제거 또는 주석 처리
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query; // 제거 또는 주석 처리
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // roomId로 채팅방을 찾는 메소드
    Optional<ChatRoom> findByRoomId(String roomId);

    // `findChatRoomByUsersAndType` 메소드 제거
}
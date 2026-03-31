package com.backend.project.repository;

import com.backend.project.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomId(String roomId);

    // roomId 기준, 정원 미만일 때만 participantCount +1 (단일 UPDATE).
    @Modifying
    @Query("UPDATE ChatRoom c SET c.participantCount = c.participantCount + 1 " +
            "WHERE c.roomId = :roomId AND c.participantCount < c.maxCapacity")
    int incrementParticipantCountByRoomId(@Param("roomId") String roomId);

    // participantCount > 0일 때만 -1 (음수 방지).
    @Modifying
    @Query("UPDATE ChatRoom c SET c.participantCount = c.participantCount - 1 WHERE c.roomId = :roomId AND c.participantCount > 0")
    int decrementParticipantCountByRoomId(@Param("roomId") String roomId);
}
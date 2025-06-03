package com.backend.project.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // (선택적) 사용자가 채팅방에 참여한 시간 등 추가 정보
    // private LocalDateTime joinedAt;

    // 생성자, getter, setter 등 (Lombok 사용)

    // equals() 및 hashCode() 구현 (필요에 따라)
    // @Override
    // public boolean equals(Object o) { ... }
    // @Override
    // public int hashCode() { ... }
}
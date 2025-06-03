package com.backend.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // ChatMessage는 하나의 ChatRoom에 속합니다.
    @JoinColumn(name = "chat_room_id", nullable = false) // 외래 키 설정
    private ChatRoom chatRoom; // 어떤 채팅방의 메시지인지

    @Column(nullable = false)
    private String senderNickname; // 메시지를 보낸 사용자의 닉네임

    // private Long senderUserId; // 필요하다면 보낸 사용자의 ID도 저장할 수 있습니다.

    @Column(columnDefinition = "TEXT") // 긴 메시지 내용을 위해 TEXT 타입 사용
    private String content; // 메시지 내용

    @Column(nullable = false)
    private LocalDateTime timestamp; // 메시지 발송 시간

    // originalLanguage 필드 제거

    @PrePersist // 엔티티가 저장되기 전에 호출됩니다.
    protected void onCreate() {
        timestamp = LocalDateTime.now(); // 현재 시간으로 자동 설정
    }

}
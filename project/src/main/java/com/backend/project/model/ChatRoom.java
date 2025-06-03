package com.backend.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import com.fasterxml.jackson.annotation.JsonIgnore; // 추가
import jakarta.persistence.EnumType; // 추가
import jakarta.persistence.Enumerated; // 추가
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany; // 추가
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List; // 추가

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 1:1 채팅방 또는 그룹 채팅방 정보를 나타내는 엔티티
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomId; // UUID 형태의 고유한 방 ID

    @Column(nullable = false)
    private String name; // 채팅방 이름 (1:1 채팅방의 경우 "[사용자1 닉네임] 님과 [사용자2 닉네임] 님의 대화" 형태)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING) // Enum 타입을 문자열로 저장
    private ChatRoomType type; // 채팅방 타입 (ONE_TO_ONE, GROUP)

    // 채팅방 타입을 위한 Enum
    public enum ChatRoomType {
        ONE_TO_ONE, GROUP
    }

    @OneToMany(mappedBy = "chatRoom") // ChatRoomParticipant 엔티티의 chatRoom 필드에 의해 매핑됨
    @JsonIgnore // JSON 직렬화 시 이 필드를 무시하여 순환 참조 및 불필요한 데이터 전송 방지
    private List<ChatRoomParticipant> participants;
}
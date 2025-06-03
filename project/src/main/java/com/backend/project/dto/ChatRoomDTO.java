package com.backend.project.dto;

import com.backend.project.model.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private String roomId;
    private String name;
    private ChatRoom.ChatRoomType type;

    public static ChatRoomDTO fromEntity(ChatRoom chatRoom) {
        return new ChatRoomDTO(chatRoom.getRoomId(), chatRoom.getName(), chatRoom.getType());
    }
}
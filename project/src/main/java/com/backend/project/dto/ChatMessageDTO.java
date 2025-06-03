package com.backend.project.dto; 

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String senderNickname;
    private String content;
    private LocalDateTime timestamp;
    // originalLanguage 필드 제거 (만약 추가했었다면)
    
}

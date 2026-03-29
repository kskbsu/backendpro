package com.backend.project.event;

import java.time.LocalDateTime;

public record ChatMessageSavedEvent(
        String roomId,
        Long messageId,
        String senderNickname,
        String content,
        LocalDateTime timestamp
) {}

package com.backend.project.event;

import java.time.LocalDateTime;

public record ChatMessageSavedEvent(
        String roomId,
        String senderNickname,
        String content,
        LocalDateTime timestamp
) {}

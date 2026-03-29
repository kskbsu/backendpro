package com.backend.project.event;

import com.backend.project.controller.ChatController.BroadcastMessage;
import com.backend.project.controller.ChatController.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageSavedEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageSaved(ChatMessageSavedEvent event) {
        log.info("ChatMessageSavedEvent AFTER_COMMIT: roomId={}, messageId={}", event.roomId(), event.messageId());
        try {
            BroadcastMessage broadcastMessage = new BroadcastMessage(
                    event.roomId(),
                    event.senderNickname(),
                    event.content(),
                    MessageType.CHAT,
                    event.timestamp(),
                    null
            );
            messagingTemplate.convertAndSend("/topic/room/" + event.roomId(), broadcastMessage);
            log.info("STOMP CHAT broadcast ok: roomId={}, messageId={}, destination=/topic/room/{}",
                    event.roomId(), event.messageId(), event.roomId());
        } catch (Exception e) {
            log.error("STOMP CHAT broadcast failed: roomId={}, messageId={}", event.roomId(), event.messageId(), e);
            throw e;
        }
    }
}

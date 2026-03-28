package com.backend.project.event;

import com.backend.project.controller.ChatController.BroadcastMessage;
import com.backend.project.controller.ChatController.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessageSavedEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessageSaved(ChatMessageSavedEvent event) {
        BroadcastMessage broadcastMessage = new BroadcastMessage(
                event.roomId(),
                event.senderNickname(),
                event.content(),
                MessageType.CHAT,
                event.timestamp(),
                null
        );
        messagingTemplate.convertAndSend("/topic/room/" + event.roomId(), broadcastMessage);
    }
}

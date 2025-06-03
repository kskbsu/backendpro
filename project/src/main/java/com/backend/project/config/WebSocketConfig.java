package com.backend.project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커가 /topic 또는 /queue 로 시작하는 대상(destination)을 구독하는 클라이언트에게 메시지를 전달하도록 설정합니다.
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 prefix 를 설정합니다.
        config.setApplicationDestinationPrefixes("/app");
        // 사용자 특정 메시지를 위한 prefix 설정 (기본값은 "/user/" 이지만 명시적으로 설정 가능)
        // config.setUserDestinationPrefix("/user"); // 기본값이므로 보통 생략 가능
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 프로토콜을 사용하는 WebSocket 엔드포인트를 등록합니다.
        registry.addEndpoint("/ws-chat").withSockJS();
    }
}

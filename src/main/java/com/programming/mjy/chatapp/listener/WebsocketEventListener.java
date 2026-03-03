package com.programming.mjy.chatapp.listener;

import com.programming.mjy.chatapp.dto.ChatMessage;
import com.programming.mjy.chatapp.dto.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketEventListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setMessageType(MessageType.LEAVE);
            chatMessage.setUserName(username);
            chatMessage.setMessage(username + " left the chat");
            log.info("User disconnected: {}", username);
            redisTemplate.convertAndSend("chat", chatMessage);
        }
    }
}

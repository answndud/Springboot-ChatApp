package com.programming.mjy.chatapp.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.programming.mjy.chatapp.dto.ChatMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final RedisTemplate<String, Object> redisTemplate; // Redis에서 수신한 byte[] 메시지를 문자열로 역직렬화하는 데 사용
    private final ObjectMapper objectMapper; // JSON → ChatMessage 객체 변환
    private final SimpMessageSendingOperations simpMessageSendingOperations; // WebSocket(STOMP) 클라이언트에게 메시지를 브로드캐스트하는 인터페이스

    @Override
    public void onMessage(Message message, @Nullable byte[] pattern) {
        // Redis Pub/Sub에서 전달된 raw byte[] 메시지 수신
        // message.getBody()는 byte[] 형태
        // Redis에서 받은 byte[]를 문자열(JSON)로 역직렬화
        String publishedMessage =  redisTemplate.getStringSerializer().deserialize(message.getBody());

        try {
            // JSON 문자열을 ChatMessage 객체로 변환
            ChatMessage chatMessage = objectMapper.readValue(publishedMessage, ChatMessage.class);
            simpMessageSendingOperations.convertAndSend("/topic/public", chatMessage);
        } catch (JsonProcessingException e) {
            // JSON 파싱 실패 시 런타임 예외 발생
            throw new RuntimeException(e);
        }
    }

}

package com.programming.mjy.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.programming.mjy.chatapp.listener.RedisMessageSubscriber;

@Configuration
public class RedisConfig {

    // Redis Pub/Sub 메시지를 수신하기 위한 Listener 컨테이너
    // 내부적으로 별도 쓰레드에서 Redis 채널을 subscribe하고 있다가 메시지 도착 시 리스너에게 위임
    // 구조: Redis/Dragonfly → subscribe("chat") → messageListenerAdapter → RedisMessageSubscriber.onMessage()
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory, MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(messageListenerAdapter, channelTopic());

        return container;
    }

    // 실제 메시지를 처리할 Subscriber를 Spring Redis가 이해할 수 있도록 Adapter로 감싸는 객체
    // RedisMessageSubscriber의 onMessage()를 자동으로 호출하도록 연결해주는 브릿지 역할
    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisMessageSubscriber redisMessageSubscriber) {
        return new MessageListenerAdapter(redisMessageSubscriber);
    }

    // Redis Pub/Sub에서 사용할 채널 이름 정의
    // "chat" 이라는 Redis 채널 생성, Publisher가 이 채널로 메시지 발행, Subscriber가 이 채널을 구독
    // WebSocket Client → Spring → Redis publish("chat", message) → Redis/Dragonfly → Subscriber 수신
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("chat");
    }

    // Redis(Dragonfly)와 통신하는 핵심 Client 추상화 객체
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>(); 
        template.setConnectionFactory(redisConnectionFactory); // Redis 서버 연결 정보 설정 / host, port, timeout 등 포함 / Dragonfly도 동일하게 처리됨
        template.setKeySerializer(new StringRedisSerializer()); // Key를 문자열로 저장 / Redis CLI에서 사람이 읽을 수 있음 / 운영, 디버깅 용이 
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class)); // Value를 JSON으로 직렬화 / Java 객체 ↔ JSON 변환 / WebSocket 메시지 객체 저장/전송에 적합
        return template;
    }
    
}

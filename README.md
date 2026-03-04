# Spring Boot WebSocket Chat App (with DragonflyDB)

간단한 실시간 채팅 예제입니다.

- WebSocket(STOMP + SockJS)로 클라이언트와 실시간 통신
- Redis 프로토콜 호환 DB인 DragonflyDB를 Pub/Sub 브로커로 사용
- Spring 서버 인스턴스가 여러 대여도 Redis 채널(`chat`) 기반으로 메시지 동기화 가능

## 기술 스택

- Java 21
- Spring Boot 3.5.11
- Spring WebSocket
- Spring Data Redis
- SockJS, STOMP.js, jQuery (프론트)
- DragonflyDB (docker-compose)

## 프로젝트 구조

```text
src/main/java/com/programming/mjy/chatapp
├── ChatappApplication.java
├── config
│   ├── WebsocketConfig.java
│   └── RedisConfig.java
├── control
│   └── ChatController.java
├── dto
│   ├── ChatMessage.java
│   └── MessageType.java
└── listener
    ├── RedisMessageSubscriber.java
    └── WebsocketEventListener.java

src/main/resources
├── application.properties
└── static
    └── index.html
```

## 컴포넌트 역할

- `WebsocketConfig`
  - STOMP 엔드포인트: `/chat-app` (SockJS fallback)
  - 애플리케이션 목적지 prefix: `/app/`
  - 브로커 구독 경로: `/topic/public`
- `ChatController`
  - `/app/chat.addUser`: 사용자 입장(JOIN) 메시지 생성 후 Redis 채널 publish
  - `/app/chat.sendChatMessage`: 채팅(CHAT) 메시지에 timestamp 추가 후 publish
- `RedisConfig`
  - `ChannelTopic("chat")`, `RedisTemplate`, `RedisMessageListenerContainer` 구성
- `RedisMessageSubscriber`
  - Redis Pub/Sub 메시지 수신
  - JSON 문자열을 `ChatMessage`로 역직렬화 후 `/topic/public`으로 브로드캐스트
- `WebsocketEventListener`
  - WebSocket 연결 종료 시 LEAVE 메시지를 Redis 채널로 publish
- `index.html`
  - 사용자명 입력 후 SockJS 연결
  - `/topic/public` 구독 및 메시지 렌더링

## 메시지 흐름

1. 브라우저가 `/chat-app`으로 WebSocket(STOMP) 연결
2. 클라이언트가 `/app/chat.addUser` 또는 `/app/chat.sendChatMessage`로 메시지 전송
3. `ChatController`가 메시지를 Redis(DragonflyDB) 채널 `chat`에 publish
4. `RedisMessageSubscriber`가 `chat` 채널 메시지를 수신
5. 수신 메시지를 `/topic/public`으로 브로드캐스트
6. 구독 중인 모든 클라이언트가 실시간으로 메시지 수신

## 로컬 실행

### 1) DragonflyDB 실행

```bash
docker compose up -d
```

기본 포트 매핑: `6379:6379`

### 2) 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3) 접속

브라우저에서 아래 주소 접속:

- <http://localhost:8080>

여러 탭(또는 여러 브라우저)으로 접속해서 실시간 채팅 동작을 확인할 수 있습니다.

## 설정 포인트

- Redis 연결 정보
  - 현재 `application.properties`에 별도 Redis host/port 설정이 없어 Spring Boot 기본값(`localhost:6379`)을 사용합니다.
  - DragonflyDB를 다른 호스트/포트로 실행하면 아래 설정을 추가하세요.

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

- 채널명 변경
  - `RedisConfig.channelTopic()`의 `chat`
  - `ChatController`, `WebsocketEventListener`의 `convertAndSend("chat", ...)`

- WebSocket/STOMP 경로 변경
  - 엔드포인트: `WebsocketConfig.registerStompEndpoints`
  - 구독 경로: `WebsocketConfig.configureMessageBroker`
  - 프론트 스크립트(`index.html`)의 send/subscribe 경로

## 테스트

```bash
./gradlew test
```

현재 테스트는 `contextLoads()` 기본 테스트 1개로 구성되어 있습니다.

## 참고

- DragonflyDB는 Redis 프로토콜 호환이므로 Spring Data Redis 설정으로 바로 연동됩니다.
- 이 구조는 단일 서버뿐 아니라 다중 서버 확장 시에도 Redis Pub/Sub를 통해 동일한 메시지 fan-out 패턴을 유지할 수 있습니다.

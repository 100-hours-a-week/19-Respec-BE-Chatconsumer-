package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.service;

import static kakaotech.bootcamp.respec.specranking.chatconsumer.domain.common.type.NotificationTargetType.CHAT;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Duration;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.consume.ChatConsumeDto;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.mapping.ChatDtoMapping;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.relay.ChatRelayDto;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.entity.Chat;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.repository.ChatRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatparticipation.entity.ChatParticipation;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatparticipation.repository.ChatParticipationRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.entity.Chatroom;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.repository.ChatroomRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.notification.entity.Notification;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.notification.repository.NotificationRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.entity.User;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRelayService {

    private static final String REDIS_USER_KEY_PREFIX = "chat:user:";
    private static final String CHAT_RELAY_API_PATH = "/api/chat/relay";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(3);
    private static final String SENT_STATUS = "SENT";

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatParticipationRepository chatParticipationRepository;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "chat")
    @Transactional
    public void handleChatMessage(String chatMessage) {
        try {
            ChatConsumeDto chatDto = objectMapper.readValue(chatMessage, ChatConsumeDto.class);

            if (!SENT_STATUS.equals(chatDto.getStatus())) {
                throw new IllegalArgumentException("Chat message is not sent");
            }

            if (!checkAndSetIdempotencyKey(chatDto.getIdempotentKey())) {
                return;
            }

            Long senderId = Long.valueOf(chatDto.getSenderId());
            Long receiverId = Long.valueOf(chatDto.getReceiverId());

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new RuntimeException("Sender not found: " + senderId));
            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found: " + receiverId));

            Chatroom chatroom = getOrCreateChatRoom(sender, receiver);

            saveChatMessage(sender, receiver, chatroom, chatDto.getContent());
            relayMessageToReceiver(receiver, chatDto);
            redisTemplate.expire(chatDto.getIdempotentKey(), IDEMPOTENCY_TTL);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
        }
    }

    private Boolean checkAndSetIdempotencyKey(String idempotentKey) {
        return redisTemplate.opsForValue().setIfAbsent(idempotentKey, "1");
    }

    private Chatroom getOrCreateChatRoom(User sender, User receiver) {
        return chatroomRepository.findCommonChatroom(sender, receiver)
                .orElseGet(() -> createNewChatRoom(sender, receiver));
    }

    private Chatroom createNewChatRoom(User sender, User receiver) {
        Chatroom chatroom = new Chatroom();
        Chatroom savedChatroom = chatroomRepository.save(chatroom);

        ChatParticipation senderParticipation = new ChatParticipation(savedChatroom, sender);
        chatParticipationRepository.save(senderParticipation);

        ChatParticipation receiverParticipation = new ChatParticipation(savedChatroom, receiver);
        chatParticipationRepository.save(receiverParticipation);

        return savedChatroom;
    }

    private void saveChatMessage(User sender, User receiver, Chatroom chatroom, String content) {
        Chat chat = new Chat(sender, receiver, chatroom, content);
        chatRepository.save(chat);
    }


    private void relayMessageToReceiver(User receiver, ChatConsumeDto chatConsumeDto) {
        String userServerIp = (String) redisTemplate.opsForValue().get(REDIS_USER_KEY_PREFIX + receiver.getId());

        ChatRelayDto chatRelayDto = ChatDtoMapping.consumeToRelay(chatConsumeDto);

        if (userServerIp != null && !userServerIp.isEmpty()) {
            relayToUserServer(userServerIp, chatRelayDto);
        } else {
            createChatNotification(receiver);
        }
    }


    private void relayToUserServer(String serverIp, ChatRelayDto chatDto) {
        try {
            String url = serverIp + CHAT_RELAY_API_PATH;

            webClient.post()
                    .uri(url)
                    .bodyValue(chatDto)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe();

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to relay chat to server: " + serverIp, e);
        }
    }

    private void createChatNotification(User receiver) {
        Notification notification = new Notification(receiver, CHAT, receiver.getId());
        notificationRepository.save(notification);
    }

}

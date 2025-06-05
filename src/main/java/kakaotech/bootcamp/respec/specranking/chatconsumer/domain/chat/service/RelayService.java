package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.service;

import static kakaotech.bootcamp.respec.specranking.chatconsumer.domain.common.type.NotificationTargetType.CHAT;

import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.relay.ChatRelayDto;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.notification.entity.Notification;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.notification.repository.NotificationRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class RelayService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    private final NotificationRepository notificationRepository;

    private static final String REDIS_USER_KEY_PREFIX = "chat:user:";
    private static final String CHAT_RELAY_API_PATH = "/api/chat/relay";

    public void relayOrNotify(User receiver, ChatRelayDto dto) {
        String serverIp = (String) redisTemplate.opsForValue().get(REDIS_USER_KEY_PREFIX + receiver.getId());
        if (serverIp != null && !serverIp.isEmpty()) {
            webClient.post()
                    .uri(serverIp + CHAT_RELAY_API_PATH)
                    .bodyValue(dto)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe();
        } else {
            notificationRepository.save(new Notification(receiver, CHAT, receiver.getId()));
        }
    }
}

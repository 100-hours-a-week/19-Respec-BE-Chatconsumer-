package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Duration;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.consume.ChatConsumeDto;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.mapping.ChatDtoMapping;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.entity.Chat;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.repository.ChatRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatparticipation.entity.ChatParticipation;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatparticipation.repository.ChatParticipationRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.entity.Chatroom;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.repository.ChatroomRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.entity.User;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.repository.UserRepository;
import kakaotech.bootcamp.respec.specranking.chatconsumer.global.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatConsumeService {

    private static final String SENT_STATUS = "SENT";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(3);

    private final ObjectMapper objectMapper;
    private final RelayService relayService;
    private final IdempotencyService idempotencyService;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatroomRepository chatroomRepository;
    private final ChatParticipationRepository chatParticipationRepository;


    @KafkaListener(topics = "chat")
    @Transactional
    public void handleChatMessage(String chatMessage) {
        try {
            ChatConsumeDto chatDto = objectMapper.readValue(chatMessage, ChatConsumeDto.class);

            if (!SENT_STATUS.equals(chatDto.getStatus())) {
                throw new IllegalArgumentException("Chat message is not sent");
            }

            if (!idempotencyService.tryAcquire(chatDto.getIdempotentKey())) {
                return;
            }

            User sender = findUser(chatDto.getSenderId());
            User receiver = findUser(chatDto.getReceiverId());

            Chatroom chatroom = getOrCreateChatRoom(sender, receiver);

            chatRepository.save(new Chat(sender, receiver, chatroom, chatDto.getContent()));

            relayService.relayOrNotify(receiver, ChatDtoMapping.consumeToRelay(chatDto));

            idempotencyService.setTtl(chatDto.getIdempotentKey(), IDEMPOTENCY_TTL);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
        }
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

    private User findUser(String idStr) {
        return userRepository.findById(Long.valueOf(idStr))
                .orElseThrow(() -> new RuntimeException("User not found: " + idStr));
    }
}

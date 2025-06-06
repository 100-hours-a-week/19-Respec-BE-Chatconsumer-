package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.relay;

import lombok.Getter;

@Getter
public class ChatRelayDto {
    private final Long senderId;
    private final Long receiverId;
    private final String content;

    public ChatRelayDto(Long senderId, Long receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }
}

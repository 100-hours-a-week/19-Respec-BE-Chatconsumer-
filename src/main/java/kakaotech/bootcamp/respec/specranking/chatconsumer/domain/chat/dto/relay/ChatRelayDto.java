package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.relay;

import lombok.Getter;

@Getter
public class ChatRelayDto {
    private final String senderId;
    private final String receiverId;
    private final String content;

    public ChatRelayDto(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }
}

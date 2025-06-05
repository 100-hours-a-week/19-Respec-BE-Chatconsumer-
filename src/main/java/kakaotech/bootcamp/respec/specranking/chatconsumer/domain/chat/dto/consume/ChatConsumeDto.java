package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.consume;

import lombok.Getter;

@Getter
public class ChatConsumeDto {
    private final String idempotentKey;
    private final Long senderId;
    private final Long receiverId;
    private final String content;
    private final String status;

    public ChatConsumeDto(String idempotentKey, Long senderId, Long receiverId, String content, String status) {
        this.idempotentKey = idempotentKey;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.status = status;
    }
}

package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.mapping;

import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.consume.ChatConsumeDto;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chat.dto.relay.ChatRelayDto;

public class ChatDtoMapping {

    public static ChatRelayDto consumeToRelay(ChatConsumeDto consumeDto) {
        return new ChatRelayDto(
                consumeDto.getSenderId(),
                consumeDto.getReceiverId(),
                consumeDto.getContent()
        );
    }
}

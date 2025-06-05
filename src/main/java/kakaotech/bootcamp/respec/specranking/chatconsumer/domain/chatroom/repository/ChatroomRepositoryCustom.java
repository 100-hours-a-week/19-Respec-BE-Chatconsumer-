package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.repository;

import java.util.Optional;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.entity.Chatroom;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.entity.User;

public interface ChatroomRepositoryCustom {
    Optional<Chatroom> findCommonChatroom(User sender, User receiver);
}

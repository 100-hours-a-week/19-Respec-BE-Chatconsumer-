package kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.repository;

import static kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.entity.QChatroom.chatroom;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatparticipation.entity.QChatParticipation;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.chatroom.entity.Chatroom;
import kakaotech.bootcamp.respec.specranking.chatconsumer.domain.user.entity.User;

class ChatroomRepositoryImpl implements ChatroomRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ChatroomRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Optional<Chatroom> findCommonChatroom(User sender, User receiver) {
        Chatroom result = queryFactory
                .selectFrom(chatroom)
                .where(
                        userParticipatesIn(sender),
                        userParticipatesIn(receiver)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private BooleanExpression userParticipatesIn(User user) {
        if (user == null) {
            return null;
        }

        QChatParticipation cp = new QChatParticipation("cp_" + user.getId());

        return JPAExpressions
                .selectOne()
                .from(cp)
                .where(
                        cp.chatroom.eq(chatroom),
                        cp.user.eq(user)
                )
                .exists();
    }
}

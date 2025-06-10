package kakaotech.bootcamp.respec.specranking.chatconsumer.global.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Boolean setIfAbsent(String key) {
        final String DUMMY_VALUE = "1";
        return redisTemplate.opsForValue().setIfAbsent(key, DUMMY_VALUE);
    }

    public void setTtl(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

}

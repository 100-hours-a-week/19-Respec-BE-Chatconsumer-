package kakaotech.bootcamp.respec.specranking.chatconsumer.global.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    public Boolean tryAcquire(String key) {
        return redisTemplate.opsForValue().setIfAbsent(key, "1");
    }

    public void setTtl(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    public void release(String key) {
        redisTemplate.delete(key);
    }
}

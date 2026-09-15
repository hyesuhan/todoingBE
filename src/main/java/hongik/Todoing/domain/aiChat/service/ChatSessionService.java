package hongik.Todoing.domain.aiChat.service;


import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String KEY_PREFIX = "chat:session:";
    private static final Duration TTL = Duration.ofDays(7);

    private final RedisTemplate<String, ChatSessionState> redisTemplate;

    public void save(String userId, ChatSessionState chatSessionState) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, chatSessionState, TTL);
    }

    public ChatSessionState get(String userId){
        return redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    }

    public void clear(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

}

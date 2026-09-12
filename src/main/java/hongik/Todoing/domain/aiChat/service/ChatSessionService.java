package hongik.Todoing.domain.aiChat.service;


import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final RedisTemplate<String, ChatSessionState> redisTemplate;

    public void save(String userId, ChatSessionState chatSessionState) {
        redisTemplate.opsForValue().set(userId, chatSessionState);
    }

    public ChatSessionState get(String userId){
        return redisTemplate.opsForValue().get(userId);
    }

    public void clear(String userId) {
        redisTemplate.delete(userId);
    }

}

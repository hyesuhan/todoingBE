package hongik.Todoing.domain.aiChat.service;


import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.domain.aiChat.dto.response.ChatSessionSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private static final String KEY_PREFIX = "chat:session:";
    private static final String REGISTRY_PREFIX = "chat:sessions:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, ChatSessionState> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public void save(String key, ChatSessionState chatSessionState) {
        redisTemplate.opsForValue().set(KEY_PREFIX + key, chatSessionState, TTL);
    }

    public ChatSessionState get(String key) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + key);
    }

    public void clear(String key) {
        redisTemplate.delete(KEY_PREFIX + key);
    }

    // 메시지가 오갈 때마다 호출 - 대화가 이어지는 한 세션이 24시간 더 연장됨
    public void extendTtl(String key) {
        redisTemplate.expire(KEY_PREFIX + key, TTL);
    }

    public void registerSession(String userId, String sessionId) {
        stringRedisTemplate.opsForSet().add(REGISTRY_PREFIX + userId, sessionId);
    }

    public List<ChatSessionSummaryDTO> listSessions(String userId) {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(REGISTRY_PREFIX + userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        List<ChatSessionSummaryDTO> result = new ArrayList<>();
        for (String sessionId : sessionIds) {
            ChatSessionState state = get(userId + ":" + sessionId);
            if (state == null) {
                // TTL 만료돼서 실제 데이터는 없는데 레지스트리에만 남아있던 것 - 정리
                stringRedisTemplate.opsForSet().remove(REGISTRY_PREFIX + userId, sessionId);
                continue;
            }
            result.add(new ChatSessionSummaryDTO(
                    sessionId, state.getCategory(), state.getLevel(), state.getStartDate(), state.getEndDate()));
        }
        return result;
    }

}

package hongik.Todoing.domain.aiChat.store;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ChatResultStore {

    private static final String KEY_PREFIX = "chat:result:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    /**
     * LLM이 만든 JSON 문자열을 그대로 저장
     */
    public void save(String userId, String response) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, response, TTL);
        System.out.println("🔥[STORE SAVE] user=" + userId + " 저장값=" + response);
    }

    /**
     * 아직 응답이 없으면 null을 반환해야 폴링이 정상 작동함
     */
    public String get(String userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        System.out.println("🔥[STORE GET] user=" + userId + " 반환값=" + value);
        return value; // null이면 null 그대로 프론트에 전달됨
    }

    /**
     * 응답은 1회성이므로 반환 직후 삭제
     */
    public void clear(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}

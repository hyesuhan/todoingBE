package hongik.Todoing.domain.aiChat.service;


import hongik.Todoing.domain.aiChat.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long TTL_HOURS = 24;  // 24시간 TTL
    private static final int MAX_HISTORY = 20; // 최대 20개 메시지 유지


    private String getKey(String userId) {
        return "chat:history:" + userId;
    }

    // 히스토리 전체 가져오기
    public List<Object> getHistory(String userId) {
        return redisTemplate.opsForList().range(getKey(userId), 0, -1);
    }

    // 새로운 메시지 추가
    public void addMessage(String userId, ChatMessageDTO message) {
        String key = getKey(userId);

        // 1) Redis 리스트 뒤에 메시지 추가
        redisTemplate.opsForList().rightPush(key, message);

        // 2) TTL 재설정 (대화가 이어질 때마다 갱신됨)
        redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);

        // 3) 최근 20개만 유지
        redisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);


    }

    // 특정 유저 히스토리 초기화
    public void clear(String userId) {
        redisTemplate.delete(getKey(userId));
    }
}

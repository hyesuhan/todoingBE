package hongik.Todoing.domain.aiChat.service;


import hongik.Todoing.domain.aiChat.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatClient chatClient;

    private static final Duration TTL = Duration.ofHours(24);  // 24시간 TTL
    private static final int KEEP_RECENT = 6;     // 최근 6개는 원문 그대로 유지
    private static final int SUMMARY_TRIGGER = 10; // 이 개수를 넘으면 오래된 (TRIGGER-KEEP_RECENT)개를 한 번에 요약

    private static final String SUMMARY_SYSTEM_PROMPT = """
            너는 챗봇과 유저의 지난 대화 중 오래돼서 원문 대신 요약으로 남기는 부분을 압축하는 역할이다.
            기존 요약과 새로 추가되는 대화 내용을 합쳐서, 이후 대화에 필요한 맥락(카테고리/목표/이미 나온 결정 사항 등)만 3문장 이내 한국어로 요약해라.
            군더더기 설명 없이 요약 내용만 출력해라.
            """;

    private String getKey(String userId) {
        return "chat:history:" + userId;
    }

    private String getSummaryKey(String userId) {
        return "chat:history:summary:" + userId;
    }

    // 히스토리 전체 가져오기 (요약 이후의 최근 원문만 남아있음)
    public List<Object> getHistory(String userId) {
        return redisTemplate.opsForList().range(getKey(userId), 0, -1);
    }

    // 오래된 대화의 요약본 (없으면 null)
    public String getSummary(String userId) {
        Object value = redisTemplate.opsForValue().get(getSummaryKey(userId));
        return value == null ? null : value.toString();
    }

    // 새로운 메시지 추가
    public void addMessage(String userId, ChatMessageDTO message) {
        String key = getKey(userId);

        // 1) Redis 리스트 뒤에 메시지 추가
        redisTemplate.opsForList().rightPush(key, message);

        // 2) TTL 재설정 (대화가 이어질 때마다 갱신됨)
        redisTemplate.expire(key, TTL);

        // 3) 임계치를 넘으면 오래된 메시지를 요약으로 압축하고 최근 것만 남김 - trouble-shooting/08 Action B
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size >= SUMMARY_TRIGGER) {
            summarizeOverflow(userId, key, (int) (size - KEEP_RECENT));
        }
    }

    private void summarizeOverflow(String userId, String key, int overflowCount) {
        List<Object> overflow = redisTemplate.opsForList().range(key, 0, overflowCount - 1);
        if (overflow == null || overflow.isEmpty()) {
            return;
        }

        try {
            String overflowText = overflow.stream()
                    .map(o -> (ChatMessageDTO) o)
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));

            String previousSummary = getSummary(userId);
            String userPrompt = (previousSummary == null ? "" : "기존 요약:\n" + previousSummary + "\n\n")
                    + "추가로 요약에 합칠 대화:\n" + overflowText;

            String newSummary = chatClient.prompt()
                    .system(SUMMARY_SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();

            if (newSummary != null && !newSummary.isBlank()) {
                redisTemplate.opsForValue().set(getSummaryKey(userId), newSummary, TTL);
                log.info("대화 이력 요약 갱신 key={}, 요약대상={}개, 요약길이={}", userId, overflow.size(), newSummary.length());
            }
        } catch (Exception e) {
            // 요약이 실패해도 대화 자체는 계속돼야 하므로 이번 턴은 요약 없이 넘어가고, 원문은 그대로 trim
            log.warn("대화 이력 요약 실패 key={} - 이번 턴은 요약 없이 진행", userId, e);
        }

        redisTemplate.opsForList().trim(key, overflowCount, -1);
    }

    // 특정 유저 히스토리 초기화
    public void clear(String userId) {
        redisTemplate.delete(getKey(userId));
        redisTemplate.delete(getSummaryKey(userId));
    }
}

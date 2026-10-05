package hongik.Todoing.domain.aiChat.service;


import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
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

    // NVIDIA/Llama 전용 토크나이저는 없어서 OpenAI cl100k_base로 근사치만 계산 - 정확한 과금용이 아니라
    // "오래된 대화를 언제 요약할지" 판단 기준이므로 근사치로 충분함(trouble-shooting/08 Action C).
    private static final Encoding ENCODING = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    private static final Duration TTL = Duration.ofHours(24);  // 24시간 TTL
    private static final int TOKEN_TRIGGER_BUDGET = 600; // 원문 이력의 토큰 합이 이걸 넘으면 요약 실행
    private static final int TOKEN_KEEP_BUDGET = 300;    // 요약 후 원문은 이 토큰 예산 안에 들어올 때까지만 유지

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

    private int countTokens(ChatMessageDTO message) {
        return ENCODING.countTokens(message.getRole() + ": " + message.getContent());
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

        // 3) 원문 이력의 토큰 합이 예산을 넘으면 오래된 것부터 요약으로 압축 - trouble-shooting/08 Action C
        //    (메시지 "개수"가 아니라 실제 토큰 수 기준이라, plan 타입처럼 유난히 긴 메시지가 껴 있어도 안전하게 걸림)
        List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
        if (list == null || list.isEmpty()) {
            return;
        }

        List<ChatMessageDTO> messages = list.stream().map(o -> (ChatMessageDTO) o).toList();
        int totalTokens = messages.stream().mapToInt(this::countTokens).sum();

        if (totalTokens > TOKEN_TRIGGER_BUDGET) {
            summarizeOverflow(userId, key, messages, totalTokens);
        }
    }

    private void summarizeOverflow(String userId, String key, List<ChatMessageDTO> messages, int totalTokens) {
        int remaining = totalTokens;
        int overflowCount = 0;
        for (ChatMessageDTO m : messages) {
            if (remaining <= TOKEN_KEEP_BUDGET) {
                break;
            }
            remaining -= countTokens(m);
            overflowCount++;
        }
        if (overflowCount == 0) {
            return;
        }

        List<ChatMessageDTO> overflow = messages.subList(0, overflowCount);

        try {
            String overflowText = overflow.stream()
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
                log.info("대화 이력 요약 갱신 key={}, 요약대상={}개(약 {}토큰), 요약길이={}",
                        userId, overflow.size(), totalTokens - remaining, newSummary.length());
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

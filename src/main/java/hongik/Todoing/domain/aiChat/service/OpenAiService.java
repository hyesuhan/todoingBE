package hongik.Todoing.domain.aiChat.service;
import hongik.Todoing.domain.aiChat.dto.ChatMessageDTO;
import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;
import hongik.Todoing.domain.aiChat.dto.response.ChatResponseDTO;
import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.global.prompt.SystemPromptLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ChatSessionService sessionService;
    private final SystemPromptLoader systemPromptLoader;
    private final ChatHistoryService chatHistoryService;

    @Value("${llm.nvidia.api-key}")
    private String apiKey;

    @Value("${llm.nvidia.base-url}")
    private String apiUrl;

    @Value("${llm.nvidia.model}")
    private String model;

    public ChatResponseDTO ask(String userId, List<ChatRequestDTO.Message> messages) {

        ChatSessionState session = sessionService.get(userId);

        String url = apiUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> fullMessages = new ArrayList<>();

        // 1. 페르소나
        fullMessages.add(Map.of(
                "role", "system",
                "content", systemPromptLoader.get("persona")
        ));

        // 2. 응답 포멧
        fullMessages.add(Map.of(
                "role", "system",
                "content", systemPromptLoader.get("format")
        ));

        // 3. 세션
        fullMessages.add(Map.of(
                "role", "system",
                "content", systemPromptLoader.getSessionFormatted(
                        session.getCategory(),
                        session.getLevel(),
                        session.getStartDate(),
                        session.getEndDate()
                )
        ));

        // 4. 이전 대화
        List<Object> historyList = chatHistoryService.getHistory(userId);
        if (historyList != null) {
            for (Object h : historyList) {
                ChatMessageDTO hist = (ChatMessageDTO) h;
                fullMessages.add(Map.of(
                        "role", hist.getRole(),
                        "content", hist.getContent()
                ));
            }
        }

        // 5. 유저 메세지 생성
        for (ChatRequestDTO.Message msg : messages) {
            fullMessages.add(Map.of(
                    "role", msg.getRole(),
                    "content", msg.getContent()
            ));
        }

        // 6. 유저 메세지 chatHistory 세션에 저장
        for (ChatRequestDTO.Message msg : messages) {
            chatHistoryService.addMessage(
                    userId,
                    new ChatMessageDTO(msg.getRole(), msg.getContent())
            );
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-5");   // temperature 제거
        body.put("messages", fullMessages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) response.getBody().get("choices");

        if (choices != null && !choices.isEmpty()) {

            Map<String, Object> firstMessage =
                    (Map<String, Object>) choices.get(0).get("message");

            String reply = (String) firstMessage.get("content");

            // assistant 메시지 Redis 저장 (return 전에 반드시 저장)
            chatHistoryService.addMessage(
                    userId,
                    new ChatMessageDTO("assistant", reply)
            );

            return new ChatResponseDTO(reply);
        }

        return new ChatResponseDTO("응답 없음.");
    }
}

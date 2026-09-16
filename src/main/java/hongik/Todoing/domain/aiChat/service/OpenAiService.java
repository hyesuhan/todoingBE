package hongik.Todoing.domain.aiChat.service;
import hongik.Todoing.domain.aiChat.dto.ChatMessageDTO;
import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;
import hongik.Todoing.domain.aiChat.dto.response.ChatResponseDTO;
import hongik.Todoing.domain.aiChat.dto.ChatSessionState;
import hongik.Todoing.global.prompt.SystemPromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenAiService {

    private final ChatClient chatClient;
    private final ChatSessionService sessionService;
    private final SystemPromptLoader systemPromptLoader;
    private final ChatHistoryService chatHistoryService;

    public OpenAiService(ChatClient.Builder chatClientBuilder,
                          ChatSessionService sessionService,
                          SystemPromptLoader systemPromptLoader,
                          ChatHistoryService chatHistoryService) {
        this.chatClient = chatClientBuilder.build();
        this.sessionService = sessionService;
        this.systemPromptLoader = systemPromptLoader;
        this.chatHistoryService = chatHistoryService;
    }

    public ChatResponseDTO ask(String userId, List<ChatRequestDTO.Message> messages) {

        ChatSessionState session = sessionService.get(userId);
        sessionService.extendTtl(userId); // 대화가 이어지는 한 세션 TTL을 24시간 뒤로 연장

        List<Message> fullMessages = new ArrayList<>();

        // 1. 페르소나
        fullMessages.add(new SystemMessage(systemPromptLoader.get("persona")));

        // 2. 응답 포멧
        fullMessages.add(new SystemMessage(systemPromptLoader.get("format")));

        // 3. 세션
        fullMessages.add(new SystemMessage(systemPromptLoader.getSessionFormatted(
                session.getCategory(),
                session.getLevel(),
                session.getStartDate(),
                session.getEndDate()
        )));

        // 4. 이전 대화
        List<Object> historyList = chatHistoryService.getHistory(userId);
        if (historyList != null) {
            for (Object h : historyList) {
                ChatMessageDTO hist = (ChatMessageDTO) h;
                fullMessages.add(toMessage(hist.getRole(), hist.getContent()));
            }
        }

        // 5. 유저 메세지 추가
        for (ChatRequestDTO.Message msg : messages) {
            fullMessages.add(toMessage(msg.getRole(), msg.getContent()));
        }

        // 6. 유저 메세지 chatHistory 세션에 저장
        for (ChatRequestDTO.Message msg : messages) {
            chatHistoryService.addMessage(
                    userId,
                    new ChatMessageDTO(msg.getRole(), msg.getContent())
            );
        }

        String reply = chatClient.prompt()
                .messages(fullMessages)
                .call()
                .content();

        if (reply == null) {
            return new ChatResponseDTO("응답 없음.");
        }

        // assistant 메시지 Redis 저장 (return 전에 반드시 저장)
        chatHistoryService.addMessage(
                userId,
                new ChatMessageDTO("assistant", reply)
        );

        return new ChatResponseDTO(reply);
    }

    private Message toMessage(String role, String content) {
        return switch (role) {
            case "system" -> new SystemMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> new UserMessage(content);
        };
    }
}

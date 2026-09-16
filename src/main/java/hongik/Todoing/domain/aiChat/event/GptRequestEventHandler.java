package hongik.Todoing.domain.aiChat.event;

import hongik.Todoing.domain.aiChat.service.OpenAiService;
import hongik.Todoing.domain.aiChat.store.ChatResultStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
@RequiredArgsConstructor
public class GptRequestEventHandler {

    private final OpenAiService openAiService;
    private final ThreadPoolExecutor llmExecutor;
    private final ChatResultStore chatResultStore;

    @EventListener
    public void handleGptRequest(GptRequestEvent event) {
        llmExecutor.submit(() -> {
            try {
                log.info("GPT 요청 처리 시작 key={}, messages={}", event.key(), event.messages().size());

                String result = openAiService.ask(event.key(), event.messages()).prompt();

                log.info("GPT 요청 처리 완료 key={}", event.key());
                chatResultStore.save(event.key(), result);

            } catch (Exception e) {
                log.error("GPT 처리 스레드에서 예외 발생 key={}", event.key(), e);
            }
        });
    }
}

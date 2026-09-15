package hongik.Todoing.domain.aiChat.event;

import hongik.Todoing.domain.aiChat.service.OpenAiService;
import hongik.Todoing.domain.aiChat.store.ChatResultStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

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
                System.out.println("\n🔥[EVENT FIRED] key = " + event.key() +
                        ", messages = " + event.messages().size());

                String result = openAiService.ask(event.key(), event.messages()).prompt();

                System.out.println("result 도 프린트 했아욤");
                chatResultStore.save(event.key(), result);

            } catch (Exception e) {
                System.out.println("🔥🔥 GPT 처리 스레드에서 예외 발생!!!");
                e.printStackTrace();
            }
        });
    }
}

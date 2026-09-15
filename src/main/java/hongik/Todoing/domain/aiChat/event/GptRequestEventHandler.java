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
        // GPT 호출(블로킹 HTTP)을 이벤트 발행 스레드(디바운스 스케줄러, 1개)에서 떼어내
        // llmExecutor(5스레드)로 넘김 - trouble-shooting/01-gpt-call-blocks-thread.md
        llmExecutor.submit(() -> {
            try {
                System.out.println("\n🔥[EVENT FIRED] user = " + event.userId() +
                        ", messages = " + event.messages().size());

                String result = openAiService.ask(event.userId(), event.messages()).prompt();

                System.out.println("result 도 프린트 했아욤");
                chatResultStore.save(event.userId(), result);

            } catch (Exception e) {
                System.out.println("🔥🔥 GPT 처리 스레드에서 예외 발생!!!");
                e.printStackTrace();
            }
        });
    }
}

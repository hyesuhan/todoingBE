package hongik.Todoing.infrastructure.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class SystemPromptLoader {

    private final Map<String, String> sections = new HashMap<>();

    public SystemPromptLoader() {
        try {
            ClassPathResource resource = new ClassPathResource("prompt/prompt.txt");
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            String[] parts = content.split("--(PERSONA|FORMAT|SESSION)--");
            if (parts.length < 4) {
                throw new IllegalStateException("prompt.txt 형식이 잘못되었습니다");
            }

            sections.put("persona", parts[1].trim());
            sections.put("format", parts[2].trim());
            sections.put("session", parts[3].trim());

        } catch (Exception e) {
            throw new RuntimeException("시스템 프롬프트 파일 로딩 실패", e);
        }
    }

    public String get(String key) {
        return sections.get(key);
    }

    public String getSessionFormatted(String category, String level, String start, String end) {
        return sections.get("session")
                .replace("{{category}}", category)
                .replace("{{level}}", level)
                .replace("{{start_date}}", start)
                .replace("{{end_date}}", end);
    }
}

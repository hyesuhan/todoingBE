package hongik.Todoing.domain.aiChat.util;

import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatBufferManager {
    private final Map<String, List<ChatRequestDTO.Message>> buffer = new ConcurrentHashMap<>();

    public void add(String userId, ChatRequestDTO.Message message) {
        buffer.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
    }

    public List<ChatRequestDTO.Message> drain(String userId) {
        List<ChatRequestDTO.Message> messages = buffer.remove(userId);
        return messages != null ? messages : List.of(); // 없으면 빈 리스트를 보낸다.
    }
}

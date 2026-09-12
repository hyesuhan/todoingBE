package hongik.Todoing.domain.aiChat.event;

import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;

import java.util.List;

public record GptRequestEvent(String userId, List<ChatRequestDTO.Message> messages) {
}
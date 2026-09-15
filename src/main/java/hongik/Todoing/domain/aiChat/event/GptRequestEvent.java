package hongik.Todoing.domain.aiChat.event;

import hongik.Todoing.domain.aiChat.dto.request.ChatRequestDTO;

import java.util.List;

// key = "userId:sessionId" 조합 - 버퍼/타이머/결과스토어/세션/히스토리가 전부 이 키로 저장됨
public record GptRequestEvent(String key, List<ChatRequestDTO.Message> messages) {
}
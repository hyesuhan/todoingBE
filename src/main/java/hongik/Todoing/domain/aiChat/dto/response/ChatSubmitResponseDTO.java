package hongik.Todoing.domain.aiChat.dto.response;

public record ChatSubmitResponseDTO(
        String message,
        String sessionId,
        String pollingUrl
) {
    public static ChatSubmitResponseDTO of(String sessionId) {
        return new ChatSubmitResponseDTO(
                "메시지 수신 완료, 처리 중입니다.",
                sessionId,
                "/chat/" + sessionId + "/result"
        );
    }
}

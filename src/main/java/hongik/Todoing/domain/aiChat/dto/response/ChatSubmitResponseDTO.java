package hongik.Todoing.domain.aiChat.dto.response;

public record ChatSubmitResponseDTO(
        String message,
        String userId,
        String pollingUrl
) {
    public static ChatSubmitResponseDTO of(String userId) {
        return new ChatSubmitResponseDTO(
                "메시지 수신 완료, 처리 중입니다.",
                userId,
                "/api/chat/result/" + userId
        );
    }
}

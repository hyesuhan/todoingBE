package hongik.Todoing.domain.aiChat.dto.response;

public record ChatSessionSummaryDTO(
        String sessionId,
        String category,
        String level,
        String startDate,
        String endDate
) {
}

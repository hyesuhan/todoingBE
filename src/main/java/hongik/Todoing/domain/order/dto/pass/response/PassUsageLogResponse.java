package hongik.Todoing.domain.order.dto.pass.response;

import java.time.LocalDateTime;

public record PassUsageLogResponse(Long id, String productName, LocalDateTime usedAt) {
}

package hongik.Todoing.domain.order.dto.order.response;

import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long orderId,
        String itemName,
        Integer totalAmount,
        String orderStatus,
        LocalDateTime approvedAt
) {
}

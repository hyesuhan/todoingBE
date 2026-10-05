package hongik.Todoing.domain.order.dto.order.response;

import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.infrastructure.config.PaymentsProperties;
import lombok.Getter;

import java.util.Map;

@Getter
public class KakaoApprovePayload {
    private final String cid;
    private final String tid;
    private final String partnerOrderId;
    private final String partnerUserId;
    private final String pgToken;

    public Map<String, Object> toApproveMap(String pgToken) {
        return Map.of(
                "cid", cid,
                "tid", tid,
                "partner_order_id", partnerOrderId,
                "partner_user_id", partnerUserId,
                "pg_token", pgToken
        );
    }

    public KakaoApprovePayload(Order order, PaymentsProperties properties, String pgToken) {
        this.cid = properties.getCid();
        this.tid = order.getTid();
        this.partnerOrderId = order.getPartnerOrderId();
        this.partnerUserId = String.valueOf(order.getUserId());
        this.pgToken = pgToken;
    }
}

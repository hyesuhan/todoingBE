package hongik.Todoing.domain.order.dto.order.response;

import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.dto.order.request.KakaoReadyRequest;
import hongik.Todoing.infrastructure.config.PaymentsProperties;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class KakaoReadyPayload {
    private final String cid;
    private final String partnerOrderId;
    private final String partnerUserId;
    private final String itemName;
    private final int quantity;
    private final int totalAmount;
    private final int taxFreeAmount;
    private final String approvalUrl;
    private final String cancelUrl;
    private final String failUrl;

    public Map<String, Object> toReadyMap() {
        return Map.of(
                "cid", cid,
                "partner_order_id", partnerOrderId,
                "partner_user_id", partnerUserId,
                "item_name", itemName,
                "quantity", quantity,
                "total_amount", totalAmount,
                "tax_free_amount", taxFreeAmount,
                "approval_url", approvalUrl,
                "cancel_url", cancelUrl,
                "fail_url", failUrl
        );
    }

    public KakaoReadyPayload(String cid,
                             String partnerOrderId,
                             String partnerUserId,
                             String itemName,
                             int quantity,
                             int totalAmount,
                             int taxFreeAmount,
                             String approvalUrl,
                             String cancelUrl,
                             String failUrl) {
        this.cid = cid;
        this.partnerOrderId = partnerOrderId;
        this.partnerUserId = partnerUserId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.taxFreeAmount = taxFreeAmount;
        this.approvalUrl = approvalUrl;
        this.cancelUrl = cancelUrl;
        this.failUrl = failUrl;
    }

    public KakaoReadyPayload(Order order, PaymentsProperties properties) {
        this.cid = properties.getCid();
        this.partnerOrderId = order.getPartnerOrderId();
        this.partnerUserId = String.valueOf(order.getUserId());
        this.itemName = order.getItemCode().getName();
        this.quantity = order.getQuantity();
        this.totalAmount = order.getTotalAmount();
        this.taxFreeAmount = 0; // Assuming tax-free amount is 0 for simplicity
        this.approvalUrl = properties.getLocalApproveUrl();
        this.cancelUrl = properties.getLocalCancelRedirect();
        this.failUrl = properties.getLocalFailRedirect();
    }

    public KakaoReadyPayload(KakaoReadyRequest request, PaymentsProperties properties) {
        this.cid = properties.getCid();
        this.partnerOrderId = request.getPartnerOrderId();
        this.partnerUserId = String.valueOf(request.getUserId());
        this.itemName = request.getProductCode().getName();
        this.quantity = request.getQuantity();
        this.totalAmount = request.getProductCode().getPrice() * getQuantity();
        this.taxFreeAmount = 0;
        this.approvalUrl = properties.getLocalApproveUrl();
        this.cancelUrl = properties.getLocalCancelRedirect();
        this.failUrl = properties.getLocalFailRedirect();
    }
}

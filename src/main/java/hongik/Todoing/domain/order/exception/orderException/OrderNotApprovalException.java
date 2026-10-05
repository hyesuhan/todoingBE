package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderNotApprovalException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderNotApprovalException();

    public OrderNotApprovalException() {
        super(OrderErrorCode.ORDER_NOT_APPROVAL);
    }
}

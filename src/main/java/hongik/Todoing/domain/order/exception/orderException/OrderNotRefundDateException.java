package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderNotRefundDateException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderNotRefundDateException();

    public OrderNotRefundDateException() {
        super(OrderErrorCode.ORDER_NOT_REFUND_DATE);
    }
}

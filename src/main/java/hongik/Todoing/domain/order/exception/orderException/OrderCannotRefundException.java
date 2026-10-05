package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderCannotRefundException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderCannotRefundException();

    public OrderCannotRefundException() {
        super(OrderErrorCode.ORDER_CANNOT_REFUND);
    }
}

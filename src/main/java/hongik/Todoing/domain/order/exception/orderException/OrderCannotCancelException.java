package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderCannotCancelException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderCannotCancelException();

    public OrderCannotCancelException() {
        super(OrderErrorCode.ORDER_CANNOT_CANCEL);
    }
}

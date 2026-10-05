package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderNotValidException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderNotValidException();

    public OrderNotValidException() {
        super(OrderErrorCode.ORDER_NOT_VALID);
    }
}

package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderNotPaymentException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderNotPaymentException();

    public OrderNotPaymentException() {
        super(OrderErrorCode.ORDER_NOT_PAYMENT);
    }
}

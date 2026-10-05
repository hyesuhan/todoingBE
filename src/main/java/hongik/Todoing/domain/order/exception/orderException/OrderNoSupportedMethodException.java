package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class OrderNoSupportedMethodException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new OrderNoSupportedMethodException();

    public OrderNoSupportedMethodException() {
        super(OrderErrorCode.ORDER_NO_SUPPORTED_METHOD);
    }
}

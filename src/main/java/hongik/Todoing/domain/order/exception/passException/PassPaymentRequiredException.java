package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassPaymentRequiredException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassPaymentRequiredException();

    public PassPaymentRequiredException() {
        super(PassErrorCode.PASS_PAYMENT_REQUIRED);
    }
}

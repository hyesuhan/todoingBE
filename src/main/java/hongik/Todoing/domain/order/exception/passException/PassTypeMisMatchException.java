package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassTypeMisMatchException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassTypeMisMatchException();

    public PassTypeMisMatchException() {
        super(PassErrorCode.PASS_TYPE_MISMATCH);
    }
}

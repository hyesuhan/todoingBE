package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassNotValidException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassNotValidException();
    public PassNotValidException() {
        super(PassErrorCode.PASS_NOT_VALID);
    }
}

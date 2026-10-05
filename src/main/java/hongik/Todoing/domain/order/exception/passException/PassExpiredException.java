package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassExpiredException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassExpiredException();
    public PassExpiredException() {
        super(PassErrorCode.PASS_EXPIRED);
    }
}

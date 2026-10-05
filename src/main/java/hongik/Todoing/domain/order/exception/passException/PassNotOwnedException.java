package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassNotOwnedException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassNotOwnedException();

    public PassNotOwnedException() {
        super(PassErrorCode.PASS_NOT_OWNED);
    }
}

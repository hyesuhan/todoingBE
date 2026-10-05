package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassNotFoundException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassNotFoundException();

    public PassNotFoundException() {
        super(PassErrorCode.PASS_NOT_FOUND);
    }
}

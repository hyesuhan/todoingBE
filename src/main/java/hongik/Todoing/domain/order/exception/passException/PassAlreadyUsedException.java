package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassAlreadyUsedException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassAlreadyUsedException();
    public PassAlreadyUsedException() {
        super(PassErrorCode.PASS_ALREADY_USED);
    }
}

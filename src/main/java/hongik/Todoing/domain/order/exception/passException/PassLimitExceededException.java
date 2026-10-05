package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PassLimitExceededException extends GeneralException {
    public static final GeneralException EXCEPTION =
            new PassLimitExceededException();
    public PassLimitExceededException() {
        super(PassErrorCode.PASS_LIMIT_EXCEEDED);
    }
}

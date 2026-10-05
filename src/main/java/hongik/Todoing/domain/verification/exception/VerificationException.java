package hongik.Todoing.domain.verification.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class VerificationException extends GeneralException {

    public VerificationException(BaseErrorCode code) {
        super(code);
    }
}

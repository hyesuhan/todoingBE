package hongik.Todoing.domain.auth.exception;

import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class EmailSendFailedException extends GeneralException {
    public static final GeneralException EXCEPTION = new EmailSendFailedException();

    public EmailSendFailedException() {
        super(ErrorStatus.EMAIL_SEND_FAILED);
    }
}

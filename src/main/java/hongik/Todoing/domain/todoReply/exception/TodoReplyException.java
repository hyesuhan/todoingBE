package hongik.Todoing.domain.todoReply.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class TodoReplyException extends GeneralException {
    public TodoReplyException(BaseErrorCode code) {
        super(code);
    }
}

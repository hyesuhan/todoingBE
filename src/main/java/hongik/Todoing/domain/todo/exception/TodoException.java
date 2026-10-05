package hongik.Todoing.domain.todo.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class TodoException extends GeneralException {
    public TodoException(BaseErrorCode code) {
        super(code);
    }
}

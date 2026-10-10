package hongik.Todoing.domain.todo.label.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class LabelException extends GeneralException {
    public LabelException(BaseErrorCode code) {
        super(code);
    }
}

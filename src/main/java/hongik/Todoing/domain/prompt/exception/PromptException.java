package hongik.Todoing.domain.prompt.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class PromptException extends GeneralException {

    public PromptException(BaseErrorCode code) {
        super(code);
    }
}

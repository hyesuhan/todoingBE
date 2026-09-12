package hongik.Todoing.domain.aiChat.exception;

import hongik.Todoing.global.apiPayload.code.BaseErrorCode;
import hongik.Todoing.global.apiPayload.exception.GeneralException;

public class ChatException extends GeneralException {

    public ChatException(BaseErrorCode code) {
        super(code);
    }
}

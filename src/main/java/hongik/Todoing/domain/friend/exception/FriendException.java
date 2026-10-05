package hongik.Todoing.domain.friend.exception;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;

public class FriendException extends GeneralException {
    public FriendException(BaseErrorCode code) {
        super(code);
    }
}

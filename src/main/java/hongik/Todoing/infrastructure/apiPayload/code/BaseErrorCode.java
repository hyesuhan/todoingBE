package hongik.Todoing.infrastructure.apiPayload.code;

import hongik.Todoing.infrastructure.apiPayload.code.errorDto.ErrorReasonDTO;

public interface BaseErrorCode {

    public ErrorReasonDTO getReason();

    public ErrorReasonDTO getReasonHttpStatus();
}

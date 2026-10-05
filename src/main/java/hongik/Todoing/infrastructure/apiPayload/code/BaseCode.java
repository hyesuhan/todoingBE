package hongik.Todoing.infrastructure.apiPayload.code;

import hongik.Todoing.infrastructure.apiPayload.code.errorDto.ReasonDTO;

public interface BaseCode {
    public ReasonDTO getReason();

    public ReasonDTO getReasonHttpStatus();
}

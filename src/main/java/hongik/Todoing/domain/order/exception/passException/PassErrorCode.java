package hongik.Todoing.domain.order.exception.passException;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.code.errorDto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PassErrorCode implements BaseErrorCode {
    PASS_NOT_VALID(HttpStatus.BAD_REQUEST, "Pass_400_1", "유효하지 않은 이용권입니다."),
    PASS_EXPIRED(HttpStatus.BAD_REQUEST, "Pass_400_2", "만료된 이용권입니다."),
    PASS_ALREADY_USED(HttpStatus.BAD_REQUEST, "Pass_400_3", "이미 사용한 이용권입니다."),
    PASS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Pass_400_4", "이용권 사용 가능 횟수를 초과했습니다."),

    PASS_NOT_FOUND(HttpStatus.NOT_FOUND, "Pass_404_1", "이용권을 찾을 수 없습니다."),

    PASS_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "Pass_400_5", "이용권 유형이 맞지 않습니다."),
    PASS_PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "Pass_402_1", "이용권 결제가 필요합니다."),

    PASS_ALREADY_ACTIVE(HttpStatus.CONFLICT, "Pass_409_1", "이미 활성화된 이용권이 존재합니다."),

    PASS_NOT_OWNED(HttpStatus.FORBIDDEN, "Pass_403_1", "본인 소유의 이용권이 아닙니다."),
    PASS_PURCHASE_LIMIT(HttpStatus.BAD_REQUEST, "Pass_400_6", "이용권 구매 제한 횟수를 초과했습니다.")

    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).httpStatus(httpStatus).build();
    }
}

package hongik.Todoing.domain.order.exception.orderException;

import hongik.Todoing.infrastructure.apiPayload.code.BaseErrorCode;
import hongik.Todoing.infrastructure.apiPayload.code.errorDto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_NOT_VALID(HttpStatus.BAD_REQUEST, "Order_400_1", "유효하지 않은 주문입니다."),
    ORDER_NO_SUPPORTED_METHOD(HttpStatus.BAD_REQUEST, "Order_400_2", "지원하지 않는 주문 방식입니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "Order_400_3", "주문을 취소할 수 없습니다."),
    ORDER_CANNOT_REFUND(HttpStatus.BAD_REQUEST, "Order_400_4", "주문을 환불할 수 없습니다."),

    ORDER_NOT_APPROVAL(HttpStatus.BAD_REQUEST, "Order_400_5", "승인 주문이 아닙니다"),
    ORDER_NOT_PAYMENT(HttpStatus.BAD_REQUEST, "Order_400_6", "결제 주문이 아닙니다"),

    ORDER_NOT_REFUND_DATE(HttpStatus.BAD_REQUEST, "Order_400_7", "환불 가능한 날짜를 지났습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order_404_1", "주문을 찾을 수 없습니다."),
    ORDER_NOT_FREE(HttpStatus.BAD_REQUEST, "Order_400_8", "무료 주문이 아닙니다."),

    ORDER_OPTION_CHANGED(HttpStatus.BAD_REQUEST, "Order_400_9", "주문 옵션이 변경되었습니다."),
    CAN_NOT_DELETE_USER_APPROVE(HttpStatus.BAD_REQUEST, "Order_400_10", "유저가 탈퇴하였습니다.")
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

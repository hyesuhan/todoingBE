package hongik.Todoing.domain.order.validator;

import hongik.Todoing.Common.annotation.Validator;
import hongik.Todoing.domain.order.adaptor.OrderAdaptor;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.order.OrderStatus;
import hongik.Todoing.domain.order.exception.orderException.OrderNotValidException;
import lombok.RequiredArgsConstructor;

@Validator
@RequiredArgsConstructor
public class OrderValidator {

    private final OrderAdaptor orderAdaptor;

    // 여기에 주문 관련 검증 메서드를 추가할 수 있습니다.

    // 주문 생성 가능 검증

    // 승인 가능한 주문인지 조회
    public void validApprove(Order order, String tid) {
        if(tid == null || tid.isBlank())
            throw OrderNotValidException.EXCEPTION;
        if(order.getOrderStatus() != OrderStatus.READY)
            throw OrderNotValidException.EXCEPTION;
        if(order.getTotalAmount() <= 0)
            throw OrderNotValidException.EXCEPTION;
    }

    // 주문 생성 가능 여부 조회
    public void validCreate(Long userId, int quantity) {
        if(quantity <= 0)
            throw OrderNotValidException.EXCEPTION;

        // 나중에 더 추가 되어야 함
    }

    // 주문 승인 전 유저 탈퇴 여부 조회

    // 승인 가능한 주문인지 조회


    // 환불할 수 있는 주문인지 검증

    // tid가 이미 있는지 확인



}

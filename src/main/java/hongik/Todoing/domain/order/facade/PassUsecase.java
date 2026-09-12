package hongik.Todoing.domain.order.facade;

import hongik.Todoing.Common.annotation.UseCase;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.service.OrderService;
import hongik.Todoing.domain.order.service.PassService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PassUsecase {

    private final OrderService orderService;
    private final PassService passService;

    // 결제 준비 중
    @Transactional
    public KakaoReadyResponse requestOrderPass(Long userId, ProductCode productCode, int quantity) {
        // 카카오페이 ready
        return orderService.readyOrder(userId, productCode, quantity);
    }

    // 결제 완료
    @Transactional
    public void completeOrderPass(Long userId, String tid, String pgToken) {
        // 주문 승인
        Order order = orderService.approveOrder(userId, tid, pgToken);

        // 패스 발급
        passService.createPass(userId, order);
    }

}

package hongik.Todoing.domain.order.service;

import hongik.Todoing.Common.annotation.DomainService;
import hongik.Todoing.domain.order.adaptor.KakaoPayAdaptor;
import hongik.Todoing.domain.order.adaptor.OrderAdaptor;
import hongik.Todoing.domain.order.adaptor.OrderUuidGenerator;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.request.KakaoReadyRequest;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.dto.order.response.OrderSummaryResponse;
import hongik.Todoing.domain.order.validator.OrderValidator;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@DomainService
@Transactional(readOnly = true)
public class OrderService {

    private final OrderAdaptor orderAdaptor;
    private final OrderValidator orderValidator;
    private final OrderUuidGenerator uuidGenerator;
    private final KakaoPayAdaptor kakaoPayAdaptor;

    // Ready 상태
    @Transactional
    public KakaoReadyResponse readyOrder(Long userId, ProductCode product, int quantity) {

        // 가맹점 주문 번호 생성
        String partnerOrderId = uuidGenerator.forUser(userId, product.getName());

        // 주문을 생성합니다.
        // 주문 생성 전 dto 패턴으로 만들고 난 후에 결제 요청 준비가 완료 되면 저장하게 됩니다.
        KakaoReadyRequest request = new KakaoReadyRequest(userId, product, quantity, partnerOrderId);

        // Order order = Order.createReady(userId, product, quantity, partnerOrderId, orderValidator);

        // 카카오페이 결제 요청을 위한 준비
        KakaoReadyResponse response =  kakaoPayAdaptor.readyWithDto(request);

        // 승인이 완료되면,'READY' 상태의 주문을 저장합니다.
        Order order = Order.createReady(userId, product, quantity, partnerOrderId, orderValidator);


        // 주문에 결제 정보 저장
        orderAdaptor.saveWithTid(order, response.getTid());

        // 주문을 저장합니다.
        return response;
    }

    // 주문 승인
    @Transactional
    public Order approveOrder(Long userId, String tid, String pgToken) {
        // 주문을 찾습니다.
        Order order = orderAdaptor.findByTid(tid);

        // 본인 주문인지 확인합니다.
        if (!order.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }

        // 주문 성공
        kakaoPayAdaptor.approve(order, pgToken);

        LocalDateTime now = LocalDateTime.now();
        return order.approve(now, orderValidator);
    }

    // 주문 승인 전 취소
    public void cancelOrder(Order order) {
        LocalDateTime now = LocalDateTime.now();
        order.cancelBeforeApprove(now);
        orderAdaptor.save(order);
    }

    // 주문 실패
    public void failOrder(Order order) {
        LocalDateTime now = LocalDateTime.now();
        order.failBeforeApprove(now);
        orderAdaptor.save(order);
    }

    // 주문 승인 후 환불
    public void refundOrder(Order order) {
        LocalDateTime now = LocalDateTime.now();
        order.refund(now);
        orderAdaptor.save(order);
    }

    public List<OrderSummaryResponse> getMyOrders(Long userId) {
        return orderAdaptor.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getItemCode().getName(),
                        order.getTotalAmount(),
                        order.getOrderStatus().name(),
                        order.getApprovedAt()
                ))
                .toList();
    }
}

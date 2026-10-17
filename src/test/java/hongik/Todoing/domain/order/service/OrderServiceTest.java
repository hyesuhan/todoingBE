package hongik.Todoing.domain.order.service;

import hongik.Todoing.domain.order.adaptor.KakaoPayAdaptor;
import hongik.Todoing.domain.order.adaptor.OrderAdaptor;
import hongik.Todoing.domain.order.adaptor.OrderUuidGenerator;
import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.response.OrderSummaryResponse;
import hongik.Todoing.domain.order.validator.OrderValidator;
import hongik.Todoing.infrastructure.apiPayload.code.status.ErrorStatus;
import hongik.Todoing.infrastructure.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderAdaptor orderAdaptor;
    @Mock private OrderValidator orderValidator;
    @Mock private OrderUuidGenerator uuidGenerator;
    @Mock private KakaoPayAdaptor kakaoPayAdaptor;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderAdaptor, orderValidator, uuidGenerator, kakaoPayAdaptor);
    }

    @Test
    void getMyOrders_shouldMapOrdersToSummaryNewestFirst() {
        Order older = Order.createReady(1L, ProductCode.PASS_1, 1, "P1261001AAAAA", orderValidator);
        Order newer = Order.createReady(1L, ProductCode.PASS_1, 1, "P1261010BBBBB", orderValidator);
        // 두 주문을 구분하기 위해 newer만 승인 처리 (orderStatus로 식별: READY vs APPROVED).
        newer.approve(LocalDateTime.of(2026, 10, 10, 0, 0), orderValidator);
        // @CreationTimestamp는 실제 영속화 시점에만 채워지므로, 단위 테스트에서는 직접 고정값을 넣어 정렬을 검증한다.
        ReflectionTestUtils.setField(older, "createdAt", LocalDateTime.of(2026, 10, 1, 0, 0));
        ReflectionTestUtils.setField(newer, "createdAt", LocalDateTime.of(2026, 10, 10, 0, 0));
        when(orderAdaptor.findByUserId(1L)).thenReturn(List.of(older, newer));

        List<OrderSummaryResponse> result = orderService.getMyOrders(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).orderStatus()).isEqualTo("APPROVED"); // newer가 먼저 와야 함
        assertThat(result.get(0).itemName()).isEqualTo("1회 추가권");
        assertThat(result.get(1).orderStatus()).isEqualTo("READY"); // older가 나중
    }

    @Test
    void approveOrder_whenOrderBelongsToDifferentUser_shouldThrowForbiddenAndNotCallKakaoPay() {
        Order order = Order.createReady(1L, ProductCode.PASS_1, 1, "P1261017ABCDE", orderValidator);
        when(orderAdaptor.findByTid("tid-1")).thenReturn(order);

        assertThatThrownBy(() -> orderService.approveOrder(2L, "tid-1", "pg-1"))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(ErrorStatus.FORBIDDEN);

        verify(kakaoPayAdaptor, never()).approve(any(), anyString());
    }
}

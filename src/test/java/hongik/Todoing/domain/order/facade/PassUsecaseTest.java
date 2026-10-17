package hongik.Todoing.domain.order.facade;

import hongik.Todoing.domain.order.domain.order.Order;
import hongik.Todoing.domain.order.domain.pass.ProductCode;
import hongik.Todoing.domain.order.dto.order.response.KakaoReadyResponse;
import hongik.Todoing.domain.order.service.OrderService;
import hongik.Todoing.domain.order.service.PassService;
import hongik.Todoing.domain.order.validator.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PassUsecaseTest {

    @Mock private OrderService orderService;
    @Mock private PassService passService;
    @Mock private OrderValidator orderValidator; // 테스트용 Order 객체를 만들기 위한 보조 목(PassUsecase는 사용하지 않음)
    private PassUsecase passUsecase;

    @BeforeEach
    void setUp() {
        passUsecase = new PassUsecase(orderService, passService);
    }

    @Test
    void requestOrderPass_shouldAlwaysUsePass1AndQuantityOne() {
        KakaoReadyResponse response = KakaoReadyResponse.builder().tid("tid-1").build();
        when(orderService.readyOrder(1L, ProductCode.PASS_1, 1)).thenReturn(response);

        KakaoReadyResponse result = passUsecase.requestOrderPass(1L);

        verify(orderService).readyOrder(1L, ProductCode.PASS_1, 1);
        assertThat(result).isSameAs(response);
    }

    @Test
    void completeOrderPass_shouldApproveOrderThenCreatePass() {
        Order order = Order.createReady(1L, ProductCode.PASS_1, 1, "P1261017ABCDE", orderValidator);
        when(orderService.approveOrder(1L, "tid-1", "pg-1")).thenReturn(order);

        passUsecase.completeOrderPass(1L, "tid-1", "pg-1");

        verify(orderService).approveOrder(1L, "tid-1", "pg-1");
        verify(passService).createPass(1L, order);
    }

    @Test
    void consumePass_shouldDelegateToPassService() {
        passUsecase.consumePass(1L);

        verify(passService).consumePass(1L);
    }
}
